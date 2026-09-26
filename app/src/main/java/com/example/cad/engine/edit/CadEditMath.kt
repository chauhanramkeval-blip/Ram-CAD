package com.example.cad.engine.edit

import com.example.cad.model.CadEntity
import com.example.cad.model.CadPoint2D
import java.util.UUID
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Geometric vector mathematics for CAD editing operations:
 * - Rotation around base pivot point
 * - Scaling relative to base point
 * - Line-line, line-circle, and polyline intersection solving
 * - CAD Trim (splitting and cutting segments)
 * - CAD Extend (ray projection to boundary edges)
 */
object CadEditMath {

    private const val EPSILON = 1e-4f

    fun normalizeAngle(angleDeg: Float): Float {
        var a = angleDeg % 360f
        if (a < 0f) a += 360f
        return a
    }

    /**
     * Rotates a point [p] around [center] by [angleDeg] counter-clockwise.
     */
    fun rotatePoint(p: CadPoint2D, center: CadPoint2D, angleDeg: Float): CadPoint2D {
        val rad = Math.toRadians(angleDeg.toDouble())
        val cosT = cos(rad).toFloat()
        val sinT = sin(rad).toFloat()
        val dx = p.x - center.x
        val dy = p.y - center.y
        return CadPoint2D(
            x = center.x + (dx * cosT - dy * sinT),
            y = center.y + (dx * sinT + dy * cosT)
        )
    }

    /**
     * Scales a point [p] relative to [center] by [factor].
     */
    fun scalePoint(p: CadPoint2D, center: CadPoint2D, factor: Float): CadPoint2D {
        return CadPoint2D(
            x = center.x + (p.x - center.x) * factor,
            y = center.y + (p.y - center.y) * factor
        )
    }

    /**
     * Displaces a CAD entity by ([deltaX], [deltaY]).
     */
    fun moveEntity(entity: CadEntity, deltaX: Float, deltaY: Float, newId: String? = null): CadEntity {
        val delta = CadPoint2D(deltaX, deltaY)
        val id = newId ?: entity.id
        return when (entity) {
            is CadEntity.Line -> entity.copy(id = id, start = entity.start + delta, end = entity.end + delta)
            is CadEntity.Polyline -> entity.copy(id = id, points = entity.points.map { it + delta })
            is CadEntity.Circle -> entity.copy(id = id, center = entity.center + delta)
            is CadEntity.Arc -> entity.copy(id = id, center = entity.center + delta)
            is CadEntity.Text -> entity.copy(id = id, position = entity.position + delta)
            is CadEntity.Point -> entity.copy(id = id, position = entity.position + delta)
            is CadEntity.Dimension -> entity.copy(
                id = id,
                start = entity.start + delta,
                end = entity.end + delta,
                textPoint = entity.textPoint + delta
            )
            is CadEntity.Leader -> entity.copy(
                id = id,
                arrowPoint = entity.arrowPoint + delta,
                kneePoint = entity.kneePoint + delta,
                landingEndPoint = entity.landingEndPoint + delta
            )
            is CadEntity.Arrow -> entity.copy(
                id = id,
                start = entity.start + delta,
                end = entity.end + delta
            )
            is CadEntity.RevisionCloud -> entity.copy(
                id = id,
                vertices = entity.vertices.map { it + delta }
            )
        }
    }

    /**
     * Rotates a CAD entity around [center] by [angleDeg].
     */
    fun rotateEntity(entity: CadEntity, center: CadPoint2D, angleDeg: Float, newId: String? = null): CadEntity {
        val id = newId ?: entity.id
        return when (entity) {
            is CadEntity.Line -> entity.copy(
                id = id,
                start = rotatePoint(entity.start, center, angleDeg),
                end = rotatePoint(entity.end, center, angleDeg)
            )
            is CadEntity.Polyline -> entity.copy(
                id = id,
                points = entity.points.map { rotatePoint(it, center, angleDeg) }
            )
            is CadEntity.Circle -> entity.copy(
                id = id,
                center = rotatePoint(entity.center, center, angleDeg)
            )
            is CadEntity.Arc -> entity.copy(
                id = id,
                center = rotatePoint(entity.center, center, angleDeg),
                startAngleDeg = normalizeAngle(entity.startAngleDeg + angleDeg)
            )
            is CadEntity.Text -> entity.copy(
                id = id,
                position = rotatePoint(entity.position, center, angleDeg),
                rotationDeg = normalizeAngle(entity.rotationDeg + angleDeg)
            )
            is CadEntity.Point -> entity.copy(
                id = id,
                position = rotatePoint(entity.position, center, angleDeg)
            )
            is CadEntity.Dimension -> entity.copy(
                id = id,
                start = rotatePoint(entity.start, center, angleDeg),
                end = rotatePoint(entity.end, center, angleDeg),
                textPoint = rotatePoint(entity.textPoint, center, angleDeg)
            )
            is CadEntity.Leader -> entity.copy(
                id = id,
                arrowPoint = rotatePoint(entity.arrowPoint, center, angleDeg),
                kneePoint = rotatePoint(entity.kneePoint, center, angleDeg),
                landingEndPoint = rotatePoint(entity.landingEndPoint, center, angleDeg)
            )
            is CadEntity.Arrow -> entity.copy(
                id = id,
                start = rotatePoint(entity.start, center, angleDeg),
                end = rotatePoint(entity.end, center, angleDeg)
            )
            is CadEntity.RevisionCloud -> entity.copy(
                id = id,
                vertices = entity.vertices.map { rotatePoint(it, center, angleDeg) }
            )
        }
    }

    /**
     * Scales a CAD entity relative to [center] by [factor].
     */
    fun scaleEntity(entity: CadEntity, center: CadPoint2D, factor: Float, newId: String? = null): CadEntity {
        val safeFactor = if (abs(factor) < 1e-4f) 0.01f else factor
        val id = newId ?: entity.id
        return when (entity) {
            is CadEntity.Line -> entity.copy(
                id = id,
                start = scalePoint(entity.start, center, safeFactor),
                end = scalePoint(entity.end, center, safeFactor)
            )
            is CadEntity.Polyline -> entity.copy(
                id = id,
                points = entity.points.map { scalePoint(it, center, safeFactor) }
            )
            is CadEntity.Circle -> entity.copy(
                id = id,
                center = scalePoint(entity.center, center, safeFactor),
                radius = (entity.radius * abs(safeFactor)).coerceAtLeast(0.1f)
            )
            is CadEntity.Arc -> entity.copy(
                id = id,
                center = scalePoint(entity.center, center, safeFactor),
                radius = (entity.radius * abs(safeFactor)).coerceAtLeast(0.1f)
            )
            is CadEntity.Text -> entity.copy(
                id = id,
                position = scalePoint(entity.position, center, safeFactor),
                textHeight = (entity.textHeight * abs(safeFactor)).coerceAtLeast(1f)
            )
            is CadEntity.Point -> entity.copy(
                id = id,
                position = scalePoint(entity.position, center, safeFactor)
            )
            is CadEntity.Dimension -> entity.copy(
                id = id,
                start = scalePoint(entity.start, center, safeFactor),
                end = scalePoint(entity.end, center, safeFactor),
                textPoint = scalePoint(entity.textPoint, center, safeFactor)
            )
            is CadEntity.Leader -> entity.copy(
                id = id,
                arrowPoint = scalePoint(entity.arrowPoint, center, safeFactor),
                kneePoint = scalePoint(entity.kneePoint, center, safeFactor),
                landingEndPoint = scalePoint(entity.landingEndPoint, center, safeFactor),
                textHeight = (entity.textHeight * abs(safeFactor)).coerceAtLeast(1f),
                arrowSize = (entity.arrowSize * abs(safeFactor)).coerceAtLeast(1f)
            )
            is CadEntity.Arrow -> entity.copy(
                id = id,
                start = scalePoint(entity.start, center, safeFactor),
                end = scalePoint(entity.end, center, safeFactor),
                headSize = (entity.headSize * abs(safeFactor)).coerceAtLeast(1f)
            )
            is CadEntity.RevisionCloud -> entity.copy(
                id = id,
                vertices = entity.vertices.map { scalePoint(it, center, safeFactor) },
                arcRadius = (entity.arcRadius * abs(safeFactor)).coerceAtLeast(1f)
            )
        }
    }

    /**
     * Computes intersection point of two 2D line segments [a]->[b] and [c]->[d].
     * If [requireSegmentA] is true, intersection must lie on [a]->[b] (parameter t in [0, 1]).
     * If [requireSegmentB] is true, intersection must lie on [c]->[d] (parameter u in [0, 1]).
     */
    fun lineLineIntersection(
        a: CadPoint2D,
        b: CadPoint2D,
        c: CadPoint2D,
        d: CadPoint2D,
        requireSegmentA: Boolean = true,
        requireSegmentB: Boolean = true
    ): CadPoint2D? {
        val rx = b.x - a.x
        val ry = b.y - a.y
        val sx = d.x - c.x
        val sy = d.y - c.y

        val cross = rx * sy - ry * sx
        if (abs(cross) < EPSILON) return null // Parallel or collinear

        val qx = c.x - a.x
        val qy = c.y - a.y

        val t = (qx * sy - qy * sx) / cross
        val u = (qx * ry - qy * rx) / cross

        if (requireSegmentA && (t < -EPSILON || t > 1f + EPSILON)) return null
        if (requireSegmentB && (u < -EPSILON || u > 1f + EPSILON)) return null

        return CadPoint2D(a.x + t * rx, a.y + t * ry)
    }

    /**
     * Computes intersections between line segment [a]->[b] and circle with [center] and [radius].
     */
    fun lineCircleIntersections(
        a: CadPoint2D,
        b: CadPoint2D,
        center: CadPoint2D,
        radius: Float,
        requireSegment: Boolean = true
    ): List<CadPoint2D> {
        val dx = b.x - a.x
        val dy = b.y - a.y
        val lenSq = dx * dx + dy * dy
        if (lenSq < EPSILON) return emptyList()

        val fx = a.x - center.x
        val fy = a.y - center.y

        val quadA = lenSq
        val quadB = 2f * (fx * dx + fy * dy)
        val quadC = (fx * fx + fy * fy) - radius * radius

        val discriminant = quadB * quadB - 4f * quadA * quadC
        if (discriminant < 0f) return emptyList()

        val sqrtDisc = sqrt(discriminant)
        val t1 = (-quadB - sqrtDisc) / (2f * quadA)
        val t2 = (-quadB + sqrtDisc) / (2f * quadA)

        val results = mutableListOf<CadPoint2D>()
        for (t in listOf(t1, t2)) {
            if (!requireSegment || (t in -EPSILON..(1f + EPSILON))) {
                results.add(CadPoint2D(a.x + t * dx, a.y + t * dy))
            }
        }
        return results
    }

    /**
     * Extracts all linear boundary segments from a list of entities.
     */
    fun extractLinearSegments(entities: List<CadEntity>): List<Pair<CadPoint2D, CadPoint2D>> {
        val segments = mutableListOf<Pair<CadPoint2D, CadPoint2D>>()
        for (entity in entities) {
            when (entity) {
                is CadEntity.Line -> segments.add(Pair(entity.start, entity.end))
                is CadEntity.Polyline -> {
                    for (i in 0 until entity.points.size - 1) {
                        segments.add(Pair(entity.points[i], entity.points[i + 1]))
                    }
                    if (entity.isClosed && entity.points.size > 2) {
                        segments.add(Pair(entity.points.last(), entity.points.first()))
                    }
                }
                is CadEntity.Dimension -> segments.add(Pair(entity.start, entity.end))
                else -> {}
            }
        }
        return segments
    }

    /**
     * Calculates distance from point [p] to segment [a]->[b].
     */
    fun distanceToSegment(p: CadPoint2D, a: CadPoint2D, b: CadPoint2D): Float {
        val l2 = a.distanceTo(b).let { it * it }
        if (l2 < EPSILON) return p.distanceTo(a)
        val t = ((p.x - a.x) * (b.x - a.x) + (p.y - a.y) * (b.y - a.y)) / l2
        val clampedT = t.coerceIn(0f, 1f)
        val projection = CadPoint2D(a.x + clampedT * (b.x - a.x), a.y + clampedT * (b.y - a.y))
        return p.distanceTo(projection)
    }

    /**
     * Finds all intersection points between [target] line and [cuttingEntities].
     */
    fun findIntersectionsOnLine(target: CadEntity.Line, cuttingEntities: List<CadEntity>): List<CadPoint2D> {
        val intersections = mutableListOf<CadPoint2D>()
        val segments = extractLinearSegments(cuttingEntities.filterNot { it.id == target.id })

        for ((c, d) in segments) {
            val pt = lineLineIntersection(target.start, target.end, c, d, requireSegmentA = true, requireSegmentB = true)
            if (pt != null) {
                // Ignore intersections that are too close to existing ones
                if (intersections.none { it.distanceTo(pt) < 1f }) {
                    intersections.add(pt)
                }
            }
        }

        // Also check circular cutting entities
        for (entity in cuttingEntities) {
            if (entity.id == target.id) continue
            if (entity is CadEntity.Circle) {
                val circlePts = lineCircleIntersections(target.start, target.end, entity.center, entity.radius, requireSegment = true)
                for (pt in circlePts) {
                    if (intersections.none { it.distanceTo(pt) < 1f }) {
                        intersections.add(pt)
                    }
                }
            } else if (entity is CadEntity.Arc) {
                val arcPts = lineCircleIntersections(target.start, target.end, entity.center, entity.radius, requireSegment = true)
                for (pt in arcPts) {
                    val angle = entity.center.angleTo(pt)
                    val endAngle = normalizeAngle(entity.startAngleDeg + entity.sweepAngleDeg)
                    val inSweep = if (entity.startAngleDeg <= endAngle) {
                        angle in entity.startAngleDeg..endAngle
                    } else {
                        angle >= entity.startAngleDeg || angle <= endAngle
                    }
                    if (inSweep && intersections.none { it.distanceTo(pt) < 1f }) {
                        intersections.add(pt)
                    }
                }
            }
        }

        return intersections
    }

    /**
     * CAD Trim Result containing:
     * - [replacementEntities]: The resulting trimmed entities replacing the target
     * - [removedSegment]: The removed segment (used for dynamic preview)
     */
    data class TrimResult(
        val replacementEntities: List<CadEntity>,
        val removedSegment: CadEntity.Line
    )

    /**
     * Performs CAD Trim on [target] line:
     * 1. Finds all intersections with cutting entities on the target line.
     * 2. Partitions the target line into sub-segments.
     * 3. Finds which sub-segment is closest to [clickPoint] (the clicked segment to remove).
     * 4. Returns the remaining trimmed segments and the removed segment.
     */
    fun trimLine(
        target: CadEntity.Line,
        clickPoint: CadPoint2D,
        cuttingEntities: List<CadEntity>
    ): TrimResult? {
        val rawIntersections = findIntersectionsOnLine(target, cuttingEntities)
        // Filter out intersections that are directly at endpoints
        val validIntersections = rawIntersections.filter { pt ->
            pt.distanceTo(target.start) > 2f && pt.distanceTo(target.end) > 2f
        }
        if (validIntersections.isEmpty()) return null

        // Sort intersections by distance along target.start -> target.end
        val sortedIntersections = validIntersections.sortedBy { target.start.distanceTo(it) }

        // Construct partition points: [start, I1, I2, ..., In, end]
        val points = listOf(target.start) + sortedIntersections + listOf(target.end)

        // Find which segment [points[i], points[i+1]] is closest to clickPoint
        var bestIndex = 0
        var minDistance = Float.MAX_VALUE

        for (i in 0 until points.size - 1) {
            val dist = distanceToSegment(clickPoint, points[i], points[i + 1])
            if (dist < minDistance) {
                minDistance = dist
                bestIndex = i
            }
        }

        val removedLine = CadEntity.Line(
            id = "${target.id}_cut",
            layerId = target.layerId,
            colorArgb = 0xFFEF4444, // Red preview for cut part
            start = points[bestIndex],
            end = points[bestIndex + 1],
            strokeWidth = target.strokeWidth
        )

        val remainingEntities = mutableListOf<CadEntity>()
        for (i in 0 until points.size - 1) {
            if (i == bestIndex) continue
            val segStart = points[i]
            val segEnd = points[i + 1]
            if (segStart.distanceTo(segEnd) > 1f) {
                val segId = if (remainingEntities.isEmpty()) target.id else "${target.id}_trim_${i}"
                remainingEntities.add(
                    target.copy(
                        id = segId,
                        start = segStart,
                        end = segEnd
                    )
                )
            }
        }

        return TrimResult(
            replacementEntities = remainingEntities,
            removedSegment = removedLine
        )
    }

    /**
     * CAD Extend Result containing:
     * - [extendedEntity]: The modified entity extended to boundary
     * - [extensionSegment]: The newly added extension portion (used for dynamic preview)
     */
    data class ExtendResult(
        val extendedEntity: CadEntity.Line,
        val extensionSegment: CadEntity.Line
    )

    /**
     * Performs CAD Extend on [target] line:
     * 1. Determines whether [clickPoint] is closer to start or end.
     * 2. Casts a ray from the opposite end through the clicked end.
     * 3. Intersects the ray with all boundary entities.
     * 4. Extends the line to the first valid boundary hit.
     */
    fun extendLine(
        target: CadEntity.Line,
        clickPoint: CadPoint2D,
        boundaryEntities: List<CadEntity>
    ): ExtendResult? {
        val distToStart = clickPoint.distanceTo(target.start)
        val distToEnd = clickPoint.distanceTo(target.end)

        val extendEnd = distToEnd <= distToStart
        val fixedPt = if (extendEnd) target.start else target.end
        val movingPt = if (extendEnd) target.end else target.start

        val segLength = fixedPt.distanceTo(movingPt)
        if (segLength < 1f) return null

        val rx = movingPt.x - fixedPt.x
        val ry = movingPt.y - fixedPt.y

        val segments = extractLinearSegments(boundaryEntities.filterNot { it.id == target.id })
        var closestIntersection: CadPoint2D? = null
        var minParamT = Float.MAX_VALUE

        for ((c, d) in segments) {
            val sx = d.x - c.x
            val sy = d.y - c.y
            val cross = rx * sy - ry * sx
            if (abs(cross) < EPSILON) continue

            val qx = c.x - fixedPt.x
            val qy = c.y - fixedPt.y

            val t = (qx * sy - qy * sx) / cross
            val u = (qx * ry - qy * rx) / cross

            // Must intersect ahead of moving point (t > 1.001f) and on the boundary segment (u in [0, 1])
            if (t > 1.001f && u in -EPSILON..(1f + EPSILON)) {
                if (t < minParamT) {
                    minParamT = t
                    closestIntersection = CadPoint2D(fixedPt.x + t * rx, fixedPt.y + t * ry)
                }
            }
        }

        val hit = closestIntersection ?: return null

        val extendedLine = if (extendEnd) {
            target.copy(end = hit)
        } else {
            target.copy(start = hit)
        }

        val extensionSegment = CadEntity.Line(
            id = "${target.id}_ext_preview",
            layerId = target.layerId,
            colorArgb = 0xFF00E5FF, // Cyan preview for extension
            start = movingPt,
            end = hit,
            strokeWidth = target.strokeWidth
        )

        return ExtendResult(
            extendedEntity = extendedLine,
            extensionSegment = extensionSegment
        )
    }
}
