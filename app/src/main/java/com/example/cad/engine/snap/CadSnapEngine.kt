package com.example.cad.engine.snap

import com.example.cad.model.CadBoundingBox
import com.example.cad.model.CadDocument
import com.example.cad.model.CadEntity
import com.example.cad.model.CadPoint2D
import com.example.cad.model.CadViewportTransform
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * High-performance, professional CAD Object Snap (OSNAP) detection engine.
 *
 * Implements 7 standard CAD snap modes:
 * - [CadSnapMode.ENDPOINT]
 * - [CadSnapMode.MIDPOINT]
 * - [CadSnapMode.CENTER]
 * - [CadSnapMode.INTERSECTION]
 * - [CadSnapMode.QUADRANT]
 * - [CadSnapMode.PERPENDICULAR]
 * - [CadSnapMode.NEAREST]
 *
 * Features:
 * - Zoom-aware tolerance (screen-pixel aperture converted to world scale).
 * - Spatial indexing to maintain smooth 60fps on large, complex drawings.
 * - Layer visibility filtering (ignores hidden layers).
 * - Priority-ranked candidate selection (structural points like Endpoint/Midpoint beat Nearest).
 */
class CadSnapEngine(
    private val spatialIndex: CadSnapSpatialIndex = CadSnapSpatialIndex()
) {

    /**
     * Evaluates all enabled snap modes around [cursorWorld] and returns the best candidate,
     * or null if no candidate falls within the screen-space snap tolerance.
     */
    fun findSnap(
        cursorWorld: CadPoint2D,
        cursorScreen: CadPoint2D,
        document: CadDocument,
        transform: CadViewportTransform,
        settings: CadOsnapSettings,
        anchorPoint: CadPoint2D? = null
    ): CadSnapResult? {
        if (!settings.isEnabled || settings.enabledModes.isEmpty()) {
            return null
        }

        val scale = transform.scale.coerceAtLeast(0.001f)
        val tolerancePx = settings.snapToleranceScreenPx
        val toleranceWorld = (tolerancePx / scale).coerceAtLeast(0.001f)

        // Filter visible entities
        val visibleEntities = document.entities.filter { entity ->
            val layer = document.layers[entity.layerId]
            layer == null || layer.isVisible
        }

        if (visibleEntities.isEmpty()) return null

        // Sync spatial index
        spatialIndex.sync(visibleEntities)

        // Create query search box around cursor in world coordinates
        val queryBox = CadBoundingBox(
            minX = cursorWorld.x - toleranceWorld * 1.5f,
            minY = cursorWorld.y - toleranceWorld * 1.5f,
            maxX = cursorWorld.x + toleranceWorld * 1.5f,
            maxY = cursorWorld.y + toleranceWorld * 1.5f
        )

        val candidateEntities = spatialIndex.query(queryBox)
        if (candidateEntities.isEmpty()) return null

        val candidates = mutableListOf<CadSnapResult>()

        // 1. ENDPOINT
        if (settings.isModeEnabled(CadSnapMode.ENDPOINT)) {
            detectEndpoints(candidateEntities, cursorScreen, transform, tolerancePx, candidates)
        }

        // 2. MIDPOINT
        if (settings.isModeEnabled(CadSnapMode.MIDPOINT)) {
            detectMidpoints(candidateEntities, cursorScreen, transform, tolerancePx, candidates)
        }

        // 3. CENTER
        if (settings.isModeEnabled(CadSnapMode.CENTER)) {
            detectCenters(candidateEntities, cursorScreen, transform, tolerancePx, candidates)
        }

        // 4. QUADRANT
        if (settings.isModeEnabled(CadSnapMode.QUADRANT)) {
            detectQuadrants(candidateEntities, cursorScreen, transform, tolerancePx, candidates)
        }

        // 5. PERPENDICULAR
        if (settings.isModeEnabled(CadSnapMode.PERPENDICULAR)) {
            val refPoint = anchorPoint ?: cursorWorld
            detectPerpendiculars(candidateEntities, refPoint, cursorScreen, transform, tolerancePx, candidates)
        }

        // 6. INTERSECTION
        if (settings.isModeEnabled(CadSnapMode.INTERSECTION)) {
            detectIntersections(candidateEntities, cursorScreen, transform, tolerancePx, candidates)
        }

        // 7. NEAREST (evaluated only if requested, ranked lowest)
        if (settings.isModeEnabled(CadSnapMode.NEAREST)) {
            detectNearest(candidateEntities, cursorWorld, cursorScreen, transform, tolerancePx, candidates)
        }

        if (candidates.isEmpty()) return null

        // Rank candidates: lowest priority number first (Endpoint > Midpoint > ... > Nearest),
        // then lowest screen distance
        return candidates.minWithOrNull(
            compareBy<CadSnapResult> { it.mode.priority }
                .thenBy { it.distanceScreenPx }
        )
    }

    // =========================================================================
    // 1. ENDPOINTS
    // =========================================================================
    private fun detectEndpoints(
        entities: List<CadEntity>,
        cursorScreen: CadPoint2D,
        transform: CadViewportTransform,
        tolerancePx: Float,
        out: MutableList<CadSnapResult>
    ) {
        for (entity in entities) {
            when (entity) {
                is CadEntity.Line -> {
                    checkAndAddCandidate(entity.start, CadSnapMode.ENDPOINT, entity.id, cursorScreen, transform, tolerancePx, out)
                    checkAndAddCandidate(entity.end, CadSnapMode.ENDPOINT, entity.id, cursorScreen, transform, tolerancePx, out)
                }
                is CadEntity.Polyline -> {
                    for (pt in entity.points) {
                        checkAndAddCandidate(pt, CadSnapMode.ENDPOINT, entity.id, cursorScreen, transform, tolerancePx, out)
                    }
                }
                is CadEntity.Arc -> {
                    val startRad = Math.toRadians(entity.startAngleDeg.toDouble())
                    val endAngleDeg = entity.startAngleDeg + entity.sweepAngleDeg
                    val endRad = Math.toRadians(endAngleDeg.toDouble())
                    val pStart = CadPoint2D(
                        x = (entity.center.x + entity.radius * cos(startRad)).toFloat(),
                        y = (entity.center.y + entity.radius * sin(startRad)).toFloat()
                    )
                    val pEnd = CadPoint2D(
                        x = (entity.center.x + entity.radius * cos(endRad)).toFloat(),
                        y = (entity.center.y + entity.radius * sin(endRad)).toFloat()
                    )
                    checkAndAddCandidate(pStart, CadSnapMode.ENDPOINT, entity.id, cursorScreen, transform, tolerancePx, out)
                    checkAndAddCandidate(pEnd, CadSnapMode.ENDPOINT, entity.id, cursorScreen, transform, tolerancePx, out)
                }
                is CadEntity.Dimension -> {
                    checkAndAddCandidate(entity.start, CadSnapMode.ENDPOINT, entity.id, cursorScreen, transform, tolerancePx, out)
                    checkAndAddCandidate(entity.end, CadSnapMode.ENDPOINT, entity.id, cursorScreen, transform, tolerancePx, out)
                }
                is CadEntity.Point -> {
                    checkAndAddCandidate(entity.position, CadSnapMode.ENDPOINT, entity.id, cursorScreen, transform, tolerancePx, out)
                }
                else -> {}
            }
        }
    }

    // =========================================================================
    // 2. MIDPOINTS
    // =========================================================================
    private fun detectMidpoints(
        entities: List<CadEntity>,
        cursorScreen: CadPoint2D,
        transform: CadViewportTransform,
        tolerancePx: Float,
        out: MutableList<CadSnapResult>
    ) {
        for (entity in entities) {
            when (entity) {
                is CadEntity.Line -> {
                    val mid = CadPoint2D(
                        (entity.start.x + entity.end.x) / 2f,
                        (entity.start.y + entity.end.y) / 2f
                    )
                    checkAndAddCandidate(mid, CadSnapMode.MIDPOINT, entity.id, cursorScreen, transform, tolerancePx, out)
                }
                is CadEntity.Polyline -> {
                    val pts = entity.points
                    if (pts.size >= 2) {
                        for (i in 0 until pts.size - 1) {
                            val mid = CadPoint2D((pts[i].x + pts[i + 1].x) / 2f, (pts[i].y + pts[i + 1].y) / 2f)
                            checkAndAddCandidate(mid, CadSnapMode.MIDPOINT, entity.id, cursorScreen, transform, tolerancePx, out)
                        }
                        if (entity.isClosed && pts.size >= 3) {
                            val closingMid = CadPoint2D((pts.last().x + pts.first().x) / 2f, (pts.last().y + pts.first().y) / 2f)
                            checkAndAddCandidate(closingMid, CadSnapMode.MIDPOINT, entity.id, cursorScreen, transform, tolerancePx, out)
                        }
                    }
                }
                is CadEntity.Arc -> {
                    val midAngle = entity.startAngleDeg + entity.sweepAngleDeg / 2f
                    val midRad = Math.toRadians(midAngle.toDouble())
                    val mid = CadPoint2D(
                        x = (entity.center.x + entity.radius * cos(midRad)).toFloat(),
                        y = (entity.center.y + entity.radius * sin(midRad)).toFloat()
                    )
                    checkAndAddCandidate(mid, CadSnapMode.MIDPOINT, entity.id, cursorScreen, transform, tolerancePx, out)
                }
                is CadEntity.Dimension -> {
                    val mid = CadPoint2D(
                        (entity.start.x + entity.end.x) / 2f,
                        (entity.start.y + entity.end.y) / 2f
                    )
                    checkAndAddCandidate(mid, CadSnapMode.MIDPOINT, entity.id, cursorScreen, transform, tolerancePx, out)
                }
                else -> {}
            }
        }
    }

    // =========================================================================
    // 3. CENTERS
    // =========================================================================
    private fun detectCenters(
        entities: List<CadEntity>,
        cursorScreen: CadPoint2D,
        transform: CadViewportTransform,
        tolerancePx: Float,
        out: MutableList<CadSnapResult>
    ) {
        for (entity in entities) {
            when (entity) {
                is CadEntity.Circle -> {
                    checkAndAddCandidate(entity.center, CadSnapMode.CENTER, entity.id, cursorScreen, transform, tolerancePx, out)
                }
                is CadEntity.Arc -> {
                    checkAndAddCandidate(entity.center, CadSnapMode.CENTER, entity.id, cursorScreen, transform, tolerancePx, out)
                }
                else -> {}
            }
        }
    }

    // =========================================================================
    // 4. QUADRANTS (0°, 90°, 180°, 270°)
    // =========================================================================
    private fun detectQuadrants(
        entities: List<CadEntity>,
        cursorScreen: CadPoint2D,
        transform: CadViewportTransform,
        tolerancePx: Float,
        out: MutableList<CadSnapResult>
    ) {
        val quadAngles = listOf(0f, 90f, 180f, 270f)
        for (entity in entities) {
            when (entity) {
                is CadEntity.Circle -> {
                    for (deg in quadAngles) {
                        val rad = Math.toRadians(deg.toDouble())
                        val pt = CadPoint2D(
                            (entity.center.x + entity.radius * cos(rad)).toFloat(),
                            (entity.center.y + entity.radius * sin(rad)).toFloat()
                        )
                        checkAndAddCandidate(pt, CadSnapMode.QUADRANT, entity.id, cursorScreen, transform, tolerancePx, out)
                    }
                }
                is CadEntity.Arc -> {
                    for (deg in quadAngles) {
                        if (isAngleOnArc(deg, entity.startAngleDeg, entity.sweepAngleDeg)) {
                            val rad = Math.toRadians(deg.toDouble())
                            val pt = CadPoint2D(
                                (entity.center.x + entity.radius * cos(rad)).toFloat(),
                                (entity.center.y + entity.radius * sin(rad)).toFloat()
                            )
                            checkAndAddCandidate(pt, CadSnapMode.QUADRANT, entity.id, cursorScreen, transform, tolerancePx, out)
                        }
                    }
                }
                else -> {}
            }
        }
    }

    // =========================================================================
    // 5. PERPENDICULAR
    // =========================================================================
    private fun detectPerpendiculars(
        entities: List<CadEntity>,
        refPoint: CadPoint2D,
        cursorScreen: CadPoint2D,
        transform: CadViewportTransform,
        tolerancePx: Float,
        out: MutableList<CadSnapResult>
    ) {
        for (entity in entities) {
            when (entity) {
                is CadEntity.Line -> {
                    val perp = projectPointOnSegment(refPoint, entity.start, entity.end)
                    if (perp != null && refPoint.distanceTo(perp) > 0.01f) {
                        checkAndAddCandidate(perp, CadSnapMode.PERPENDICULAR, entity.id, cursorScreen, transform, tolerancePx, out)
                    }
                }
                is CadEntity.Polyline -> {
                    val pts = entity.points
                    for (i in 0 until pts.size - 1) {
                        val perp = projectPointOnSegment(refPoint, pts[i], pts[i + 1])
                        if (perp != null && refPoint.distanceTo(perp) > 0.01f) {
                            checkAndAddCandidate(perp, CadSnapMode.PERPENDICULAR, entity.id, cursorScreen, transform, tolerancePx, out)
                        }
                    }
                    if (entity.isClosed && pts.size >= 3) {
                        val perp = projectPointOnSegment(refPoint, pts.last(), pts.first())
                        if (perp != null && refPoint.distanceTo(perp) > 0.01f) {
                            checkAndAddCandidate(perp, CadSnapMode.PERPENDICULAR, entity.id, cursorScreen, transform, tolerancePx, out)
                        }
                    }
                }
                is CadEntity.Circle -> {
                    val dist = entity.center.distanceTo(refPoint)
                    if (dist > 0.001f) {
                        val ux = (refPoint.x - entity.center.x) / dist
                        val uy = (refPoint.y - entity.center.y) / dist
                        val p1 = CadPoint2D(entity.center.x + ux * entity.radius, entity.center.y + uy * entity.radius)
                        val p2 = CadPoint2D(entity.center.x - ux * entity.radius, entity.center.y - uy * entity.radius)
                        checkAndAddCandidate(p1, CadSnapMode.PERPENDICULAR, entity.id, cursorScreen, transform, tolerancePx, out)
                        checkAndAddCandidate(p2, CadSnapMode.PERPENDICULAR, entity.id, cursorScreen, transform, tolerancePx, out)
                    }
                }
                is CadEntity.Arc -> {
                    val dist = entity.center.distanceTo(refPoint)
                    if (dist > 0.001f) {
                        val ux = (refPoint.x - entity.center.x) / dist
                        val uy = (refPoint.y - entity.center.y) / dist
                        val p1 = CadPoint2D(entity.center.x + ux * entity.radius, entity.center.y + uy * entity.radius)
                        val ang1 = Math.toDegrees(atan2(uy.toDouble(), ux.toDouble())).toFloat()
                        if (isAngleOnArc(ang1, entity.startAngleDeg, entity.sweepAngleDeg)) {
                            checkAndAddCandidate(p1, CadSnapMode.PERPENDICULAR, entity.id, cursorScreen, transform, tolerancePx, out)
                        }
                    }
                }
                else -> {}
            }
        }
    }

    // =========================================================================
    // 6. INTERSECTIONS
    // =========================================================================
    private fun detectIntersections(
        entities: List<CadEntity>,
        cursorScreen: CadPoint2D,
        transform: CadViewportTransform,
        tolerancePx: Float,
        out: MutableList<CadSnapResult>
    ) {
        if (entities.size < 2) return

        // Check pairwise intersections among candidate entities
        for (i in 0 until entities.size) {
            val e1 = entities[i]
            for (j in i + 1 until entities.size) {
                val e2 = entities[j]
                if (!e1.boundingBox.intersects(e2.boundingBox)) continue

                val interPoints = computeIntersections(e1, e2)
                for (pt in interPoints) {
                    checkAndAddCandidate(
                        point = pt,
                        mode = CadSnapMode.INTERSECTION,
                        primaryId = e1.id,
                        secondaryId = e2.id,
                        cursorScreen = cursorScreen,
                        transform = transform,
                        tolerancePx = tolerancePx,
                        out = out
                    )
                }
            }
        }
    }

    // =========================================================================
    // 7. NEAREST
    // =========================================================================
    private fun detectNearest(
        entities: List<CadEntity>,
        cursorWorld: CadPoint2D,
        cursorScreen: CadPoint2D,
        transform: CadViewportTransform,
        tolerancePx: Float,
        out: MutableList<CadSnapResult>
    ) {
        for (entity in entities) {
            val nearestPt = findClosestPointOnEntity(cursorWorld, entity) ?: continue
            checkAndAddCandidate(nearestPt, CadSnapMode.NEAREST, entity.id, cursorScreen, transform, tolerancePx, out)
        }
    }

    // =========================================================================
    // Candidate Evaluation Helper
    // =========================================================================
    private fun checkAndAddCandidate(
        point: CadPoint2D,
        mode: CadSnapMode,
        primaryId: String,
        cursorScreen: CadPoint2D,
        transform: CadViewportTransform,
        tolerancePx: Float,
        out: MutableList<CadSnapResult>,
        secondaryId: String? = null
    ) {
        val screenPt = transform.worldToScreen(point)
        val distPx = hypot(screenPt.x - cursorScreen.x, screenPt.y - cursorScreen.y)

        if (distPx <= tolerancePx) {
            out.add(
                CadSnapResult(
                    point = point,
                    mode = mode,
                    screenPoint = screenPt,
                    primaryEntityId = primaryId,
                    secondaryEntityId = secondaryId,
                    distanceScreenPx = distPx,
                    description = mode.displayName
                )
            )
        }
    }

    // =========================================================================
    // Geometric Math Utilities
    // =========================================================================

    /**
     * Projects point P perpendicularly onto segment AB. Returns projection if t in [0, 1].
     */
    private fun projectPointOnSegment(p: CadPoint2D, a: CadPoint2D, b: CadPoint2D): CadPoint2D? {
        val dx = b.x - a.x
        val dy = b.y - a.y
        val lenSq = dx * dx + dy * dy
        if (lenSq < 0.00001f) return null

        val t = ((p.x - a.x) * dx + (p.y - a.y) * dy) / lenSq
        return if (t in 0.0f..1.0f) {
            CadPoint2D(a.x + t * dx, a.y + t * dy)
        } else null
    }

    /**
     * Finds closest point on entity contour to a query point.
     */
    private fun findClosestPointOnEntity(p: CadPoint2D, entity: CadEntity): CadPoint2D? {
        return when (entity) {
            is CadEntity.Line -> closestPointOnSegment(p, entity.start, entity.end)
            is CadEntity.Polyline -> {
                val pts = entity.points
                var bestPt: CadPoint2D? = null
                var bestDist = Float.MAX_VALUE
                for (i in 0 until pts.size - 1) {
                    val cand = closestPointOnSegment(p, pts[i], pts[i + 1])
                    val d = p.distanceTo(cand)
                    if (d < bestDist) {
                        bestDist = d
                        bestPt = cand
                    }
                }
                if (entity.isClosed && pts.size >= 3) {
                    val cand = closestPointOnSegment(p, pts.last(), pts.first())
                    val d = p.distanceTo(cand)
                    if (d < bestDist) {
                        bestPt = cand
                    }
                }
                bestPt
            }
            is CadEntity.Circle -> {
                val dist = entity.center.distanceTo(p)
                if (dist < 0.0001f) {
                    CadPoint2D(entity.center.x + entity.radius, entity.center.y)
                } else {
                    val ux = (p.x - entity.center.x) / dist
                    val uy = (p.y - entity.center.y) / dist
                    CadPoint2D(entity.center.x + ux * entity.radius, entity.center.y + uy * entity.radius)
                }
            }
            is CadEntity.Arc -> {
                val dist = entity.center.distanceTo(p)
                val angleDeg = if (dist < 0.0001f) {
                    entity.startAngleDeg
                } else {
                    val ang = Math.toDegrees(atan2((p.y - entity.center.y).toDouble(), (p.x - entity.center.x).toDouble())).toFloat()
                    normalizeDegrees(ang)
                }
                if (isAngleOnArc(angleDeg, entity.startAngleDeg, entity.sweepAngleDeg)) {
                    val rad = Math.toRadians(angleDeg.toDouble())
                    CadPoint2D(
                        (entity.center.x + entity.radius * cos(rad)).toFloat(),
                        (entity.center.y + entity.radius * sin(rad)).toFloat()
                    )
                } else {
                    // Snap to closest arc endpoint
                    val radStart = Math.toRadians(entity.startAngleDeg.toDouble())
                    val pStart = CadPoint2D(
                        (entity.center.x + entity.radius * cos(radStart)).toFloat(),
                        (entity.center.y + entity.radius * sin(radStart)).toFloat()
                    )
                    val radEnd = Math.toRadians((entity.startAngleDeg + entity.sweepAngleDeg).toDouble())
                    val pEnd = CadPoint2D(
                        (entity.center.x + entity.radius * cos(radEnd)).toFloat(),
                        (entity.center.y + entity.radius * sin(radEnd)).toFloat()
                    )
                    if (p.distanceTo(pStart) < p.distanceTo(pEnd)) pStart else pEnd
                }
            }
            is CadEntity.Dimension -> closestPointOnSegment(p, entity.start, entity.end)
            is CadEntity.Point -> entity.position
            else -> null
        }
    }

    private fun closestPointOnSegment(p: CadPoint2D, a: CadPoint2D, b: CadPoint2D): CadPoint2D {
        val dx = b.x - a.x
        val dy = b.y - a.y
        val lenSq = dx * dx + dy * dy
        if (lenSq < 0.00001f) return a

        val t = (((p.x - a.x) * dx + (p.y - a.y) * dy) / lenSq).coerceIn(0f, 1f)
        return CadPoint2D(a.x + t * dx, a.y + t * dy)
    }

    /**
     * Intersections between two entities.
     */
    private fun computeIntersections(e1: CadEntity, e2: CadEntity): List<CadPoint2D> {
        val segs1 = entityToSegments(e1)
        val segs2 = entityToSegments(e2)

        val results = mutableListOf<CadPoint2D>()

        // 1. Segment-Segment
        if (segs1.isNotEmpty() && segs2.isNotEmpty()) {
            for (s1 in segs1) {
                for (s2 in segs2) {
                    segmentIntersection(s1.first, s1.second, s2.first, s2.second)?.let {
                        results.add(it)
                    }
                }
            }
        }

        // 2. Segment-Circle / Segment-Arc
        if (segs1.isNotEmpty() && (e2 is CadEntity.Circle || e2 is CadEntity.Arc)) {
            val center = if (e2 is CadEntity.Circle) e2.center else (e2 as CadEntity.Arc).center
            val radius = if (e2 is CadEntity.Circle) e2.radius else (e2 as CadEntity.Arc).radius
            for (s in segs1) {
                for (pt in segmentCircleIntersections(s.first, s.second, center, radius)) {
                    if (e2 is CadEntity.Arc) {
                        val ang = Math.toDegrees(atan2((pt.y - center.y).toDouble(), (pt.x - center.x).toDouble())).toFloat()
                        if (isAngleOnArc(ang, e2.startAngleDeg, e2.sweepAngleDeg)) {
                            results.add(pt)
                        }
                    } else {
                        results.add(pt)
                    }
                }
            }
        } else if (segs2.isNotEmpty() && (e1 is CadEntity.Circle || e1 is CadEntity.Arc)) {
            val center = if (e1 is CadEntity.Circle) e1.center else (e1 as CadEntity.Arc).center
            val radius = if (e1 is CadEntity.Circle) e1.radius else (e1 as CadEntity.Arc).radius
            for (s in segs2) {
                for (pt in segmentCircleIntersections(s.first, s.second, center, radius)) {
                    if (e1 is CadEntity.Arc) {
                        val ang = Math.toDegrees(atan2((pt.y - center.y).toDouble(), (pt.x - center.x).toDouble())).toFloat()
                        if (isAngleOnArc(ang, e1.startAngleDeg, e1.sweepAngleDeg)) {
                            results.add(pt)
                        }
                    } else {
                        results.add(pt)
                    }
                }
            }
        }

        // 3. Circle-Circle / Circle-Arc / Arc-Arc
        val isCurved1 = e1 is CadEntity.Circle || e1 is CadEntity.Arc
        val isCurved2 = e2 is CadEntity.Circle || e2 is CadEntity.Arc
        if (isCurved1 && isCurved2) {
            val c1 = if (e1 is CadEntity.Circle) e1.center else (e1 as CadEntity.Arc).center
            val r1 = if (e1 is CadEntity.Circle) e1.radius else (e1 as CadEntity.Arc).radius
            val c2 = if (e2 is CadEntity.Circle) e2.center else (e2 as CadEntity.Arc).center
            val r2 = if (e2 is CadEntity.Circle) e2.radius else (e2 as CadEntity.Arc).radius

            for (pt in circleCircleIntersections(c1, r1, c2, r2)) {
                val ok1 = if (e1 is CadEntity.Arc) {
                    val a1 = Math.toDegrees(atan2((pt.y - c1.y).toDouble(), (pt.x - c1.x).toDouble())).toFloat()
                    isAngleOnArc(a1, e1.startAngleDeg, e1.sweepAngleDeg)
                } else true

                val ok2 = if (e2 is CadEntity.Arc) {
                    val a2 = Math.toDegrees(atan2((pt.y - c2.y).toDouble(), (pt.x - c2.x).toDouble())).toFloat()
                    isAngleOnArc(a2, e2.startAngleDeg, e2.sweepAngleDeg)
                } else true

                if (ok1 && ok2) {
                    results.add(pt)
                }
            }
        }

        return results
    }

    private fun entityToSegments(entity: CadEntity): List<Pair<CadPoint2D, CadPoint2D>> {
        return when (entity) {
            is CadEntity.Line -> listOf(Pair(entity.start, entity.end))
            is CadEntity.Polyline -> {
                val pts = entity.points
                val segs = mutableListOf<Pair<CadPoint2D, CadPoint2D>>()
                for (i in 0 until pts.size - 1) {
                    segs.add(Pair(pts[i], pts[i + 1]))
                }
                if (entity.isClosed && pts.size >= 3) {
                    segs.add(Pair(pts.last(), pts.first()))
                }
                segs
            }
            is CadEntity.Dimension -> listOf(Pair(entity.start, entity.end))
            else -> emptyList()
        }
    }

    /**
     * 2D segment-segment intersection using 2D cross products.
     */
    private fun segmentIntersection(
        p1: CadPoint2D, p2: CadPoint2D,
        p3: CadPoint2D, p4: CadPoint2D
    ): CadPoint2D? {
        val d1x = p2.x - p1.x
        val d1y = p2.y - p1.y
        val d2x = p4.x - p3.x
        val d2y = p4.y - p3.y

        val denom = d1x * d2y - d1y * d2x
        if (abs(denom) < 0.00001f) return null // Parallel or collinear

        val t = ((p3.x - p1.x) * d2y - (p3.y - p1.y) * d2x) / denom
        val u = ((p3.x - p1.x) * d1y - (p3.y - p1.y) * d1x) / denom

        return if (t in -0.0001f..1.0001f && u in -0.0001f..1.0001f) {
            CadPoint2D(p1.x + t.coerceIn(0f, 1f) * d1x, p1.y + t.coerceIn(0f, 1f) * d1y)
        } else null
    }

    /**
     * Segment-circle intersection points.
     */
    private fun segmentCircleIntersections(
        p1: CadPoint2D, p2: CadPoint2D,
        center: CadPoint2D, radius: Float
    ): List<CadPoint2D> {
        val dx = p2.x - p1.x
        val dy = p2.y - p1.y
        val fx = p1.x - center.x
        val fy = p1.y - center.y

        val a = dx * dx + dy * dy
        if (a < 0.00001f) return emptyList()

        val b = 2 * (fx * dx + fy * dy)
        val c = (fx * fx + fy * fy) - radius * radius

        val discriminant = b * b - 4 * a * c
        if (discriminant < 0) return emptyList()

        val rootDisc = sqrt(discriminant)
        val t1 = (-b - rootDisc) / (2 * a)
        val t2 = (-b + rootDisc) / (2 * a)

        val results = mutableListOf<CadPoint2D>()
        if (t1 in 0.0f..1.0f) {
            results.add(CadPoint2D(p1.x + t1 * dx, p1.y + t1 * dy))
        }
        if (t2 in 0.0f..1.0f && abs(t2 - t1) > 0.001f) {
            results.add(CadPoint2D(p1.x + t2 * dx, p1.y + t2 * dy))
        }
        return results
    }

    /**
     * Circle-circle intersection points.
     */
    private fun circleCircleIntersections(
        c1: CadPoint2D, r1: Float,
        c2: CadPoint2D, r2: Float
    ): List<CadPoint2D> {
        val d = c1.distanceTo(c2)
        if (d > r1 + r2 || d < abs(r1 - r2) || d < 0.0001f) return emptyList()

        val a = (r1 * r1 - r2 * r2 + d * d) / (2 * d)
        val h = sqrt(max(0f, r1 * r1 - a * a))

        val xm = c1.x + a * (c2.x - c1.x) / d
        val ym = c1.y + a * (c2.y - c1.y) / d

        val rx = -(c2.y - c1.y) * (h / d)
        val ry = (c2.x - c1.x) * (h / d)

        return if (h < 0.0001f) {
            listOf(CadPoint2D(xm, ym))
        } else {
            listOf(
                CadPoint2D(xm + rx, ym + ry),
                CadPoint2D(xm - rx, ym - ry)
            )
        }
    }

    /**
     * Determines whether an angle in degrees lies along the arc between startAngleDeg and startAngleDeg + sweepAngleDeg.
     */
    private fun isAngleOnArc(angleDeg: Float, startAngleDeg: Float, sweepAngleDeg: Float): Boolean {
        if (abs(sweepAngleDeg) >= 360f) return true

        val normTarget = normalizeDegrees(angleDeg)
        val normStart = normalizeDegrees(startAngleDeg)

        return if (sweepAngleDeg >= 0) {
            val sweep = sweepAngleDeg
            val diff = normalizeDegrees(normTarget - normStart)
            diff <= sweep + 0.01f
        } else {
            val sweep = abs(sweepAngleDeg)
            val diff = normalizeDegrees(normStart - normTarget)
            diff <= sweep + 0.01f
        }
    }

    private fun normalizeDegrees(deg: Float): Float {
        var a = deg % 360f
        if (a < 0f) a += 360f
        return a
    }
}
