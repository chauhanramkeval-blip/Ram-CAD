package com.example.cad.engine.selection

import com.example.cad.model.CadBoundingBox
import com.example.cad.model.CadEntity
import com.example.cad.model.CadPoint2D
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Geometric hit testing engine for 2D CAD entities.
 *
 * Implements high-precision hit tests for:
 * - Single-point tap picking (Line, Polyline, Circle, Arc, Text, Dimension)
 * - Window selection (enclosed entities only)
 * - Crossing selection (enclosed + intersecting entities)
 */
class HitTestEngine {

    /**
     * Hit tests a single world point against a list of CAD entities.
     * Returns the nearest entity within the given world-space tolerance.
     */
    fun hitTest(
        point: CadPoint2D,
        entities: List<CadEntity>,
        tolerance: Float
    ): CadEntity? {
        if (entities.isEmpty()) return null

        var closestEntity: CadEntity? = null
        var minDistance = Float.MAX_VALUE

        val px = point.x
        val py = point.y

        // Fast bounding-box pre-filtering for large entity sets
        val useFastBBoxPruning = entities.size > 32

        // Iterate in reverse (top-most rendered entities first)
        for (i in entities.indices.reversed()) {
            val entity = entities[i]
            val b = entity.boundingBox

            if (useFastBBoxPruning) {
                // If the point is farther than minDistance or tolerance from the bounding box, skip
                val limit = if (minDistance < tolerance) minDistance else tolerance
                if (px < b.minX - limit || px > b.maxX + limit ||
                    py < b.minY - limit || py > b.maxY + limit) {
                    continue
                }
            }

            val dist = distanceToEntity(point, entity)
            if (dist <= tolerance && dist < minDistance) {
                minDistance = dist
                closestEntity = entity
            }
        }
        return closestEntity
    }

    /**
     * Returns all entities within tolerance of the given point.
     */
    fun hitTestAll(
        point: CadPoint2D,
        entities: List<CadEntity>,
        tolerance: Float
    ): List<CadEntity> {
        if (entities.isEmpty()) return emptyList()
        val px = point.x
        val py = point.y
        val useFastBBoxPruning = entities.size > 32

        return entities.filter { entity ->
            if (useFastBBoxPruning) {
                val b = entity.boundingBox
                if (px < b.minX - tolerance || px > b.maxX + tolerance ||
                    py < b.minY - tolerance || py > b.maxY + tolerance) {
                    return@filter false
                }
            }
            distanceToEntity(point, entity) <= tolerance
        }
    }

    /**
     * Tests if an entity is hit by a point within tolerance.
     */
    fun isEntityHit(point: CadPoint2D, entity: CadEntity, tolerance: Float): Boolean {
        val b = entity.boundingBox
        val px = point.x
        val py = point.y
        if (px < b.minX - tolerance || px > b.maxX + tolerance ||
            py < b.minY - tolerance || py > b.maxY + tolerance) {
            return false
        }
        return distanceToEntity(point, entity) <= tolerance
    }

    /**
     * Calculates the minimum Euclidean distance from a point to an entity.
     */
    fun distanceToEntity(point: CadPoint2D, entity: CadEntity): Float {
        return when (entity) {
            is CadEntity.Point -> point.distanceTo(entity.position)
            is CadEntity.Line -> distancePointToSegment(point, entity.start, entity.end)
            is CadEntity.Polyline -> distancePointToPolyline(point, entity.points, entity.isClosed)
            is CadEntity.Circle -> distancePointToCircle(point, entity.center, entity.radius)
            is CadEntity.Arc -> distancePointToArc(point, entity.center, entity.radius, entity.startAngleDeg, entity.sweepAngleDeg)
            is CadEntity.Text -> distancePointToText(point, entity)
            is CadEntity.Dimension -> minOf(
                distancePointToSegment(point, entity.start, entity.end),
                point.distanceTo(entity.textPoint)
            )
            is CadEntity.Leader -> minOf(
                distancePointToSegment(point, entity.arrowPoint, entity.kneePoint),
                distancePointToSegment(point, entity.kneePoint, entity.landingEndPoint),
                point.distanceTo(entity.landingEndPoint)
            )
            is CadEntity.Arrow -> distancePointToSegment(point, entity.start, entity.end)
            is CadEntity.RevisionCloud -> {
                if (entity.vertices.isEmpty()) Float.MAX_VALUE
                else distancePointToPolyline(point, entity.vertices, entity.isClosed)
            }
        }
    }

    /**
     * Window selection: Returns entities completely enclosed inside [box].
     */
    fun windowSelect(
        box: CadBoundingBox,
        entities: List<CadEntity>
    ): List<CadEntity> {
        if (box.isEmpty || entities.isEmpty()) return emptyList()
        return entities.filter { entity ->
            val b = entity.boundingBox
            // If the entity's bounding box is fully inside box, it's definitely enclosed!
            if (box.contains(b)) return@filter true
            // If bounding box doesn't even intersect, skip!
            if (!box.intersects(b)) return@filter false
            isEntityEnclosed(entity, box)
        }
    }

    /**
     * Crossing selection: Returns entities that are inside OR intersect [box].
     */
    fun crossingSelect(
        box: CadBoundingBox,
        entities: List<CadEntity>
    ): List<CadEntity> {
        if (box.isEmpty || entities.isEmpty()) return emptyList()
        return entities.filter { entity ->
            val b = entity.boundingBox
            // If bounding box doesn't intersect selection box, it cannot cross
            if (!box.intersects(b)) return@filter false
            // If bounding box is fully inside selection box, it definitely crosses
            if (box.contains(b)) return@filter true
            isEntityCrossing(entity, box)
        }
    }

    // =========================================================================
    // Geometric Point Distance Algorithms
    // =========================================================================

    fun distancePointToSegment(p: CadPoint2D, a: CadPoint2D, b: CadPoint2D): Float {
        val dx = b.x - a.x
        val dy = b.y - a.y
        val l2 = dx * dx + dy * dy
        if (l2 == 0f) return p.distanceTo(a)

        val t = (((p.x - a.x) * dx + (p.y - a.y) * dy) / l2).coerceIn(0f, 1f)
        val projX = a.x + t * dx
        val projY = a.y + t * dy
        return hypot(p.x - projX, p.y - projY)
    }

    fun distancePointToPolyline(p: CadPoint2D, points: List<CadPoint2D>, isClosed: Boolean): Float {
        if (points.isEmpty()) return Float.MAX_VALUE
        if (points.size == 1) return p.distanceTo(points[0])

        var minDist = Float.MAX_VALUE
        for (i in 0 until points.size - 1) {
            val dist = distancePointToSegment(p, points[i], points[i + 1])
            if (dist < minDist) minDist = dist
        }
        if (isClosed && points.size > 2) {
            val dist = distancePointToSegment(p, points.last(), points.first())
            if (dist < minDist) minDist = dist
        }
        return minDist
    }

    fun distancePointToCircle(p: CadPoint2D, center: CadPoint2D, radius: Float): Float {
        val distToCenter = p.distanceTo(center)
        return abs(distToCenter - radius)
    }

    fun distancePointToArc(
        p: CadPoint2D,
        center: CadPoint2D,
        radius: Float,
        startAngleDeg: Float,
        sweepAngleDeg: Float
    ): Float {
        val distToCenter = p.distanceTo(center)
        val radialDist = abs(distToCenter - radius)

        // Calculate angle of point from center
        val rad = atan2((p.y - center.y).toDouble(), (p.x - center.x).toDouble())
        var deg = Math.toDegrees(rad).toFloat()
        if (deg < 0f) deg += 360f

        if (isAngleOnArc(deg, startAngleDeg, sweepAngleDeg)) {
            return radialDist
        }

        // If outside angular sweep, distance to nearest arc endpoint
        val startRad = Math.toRadians(startAngleDeg.toDouble())
        val endAngleDeg = startAngleDeg + sweepAngleDeg
        val endRad = Math.toRadians(endAngleDeg.toDouble())

        val startPt = CadPoint2D(
            x = center.x + radius * cos(startRad).toFloat(),
            y = center.y + radius * sin(startRad).toFloat()
        )
        val endPt = CadPoint2D(
            x = center.x + radius * cos(endRad).toFloat(),
            y = center.y + radius * sin(endRad).toFloat()
        )
        return minOf(p.distanceTo(startPt), p.distanceTo(endPt))
    }

    fun distancePointToText(p: CadPoint2D, text: CadEntity.Text): Float {
        val bounds = text.boundingBox
        if (p.x in bounds.minX..bounds.maxX && p.y in bounds.minY..bounds.maxY) {
            return 0f
        }
        // Distance to bounding box edges
        val dx = max(0f, max(bounds.minX - p.x, p.x - bounds.maxX))
        val dy = max(0f, max(bounds.minY - p.y, p.y - bounds.maxY))
        return hypot(dx, dy)
    }

    fun isAngleOnArc(angleDeg: Float, startAngleDeg: Float, sweepAngleDeg: Float): Boolean {
        if (abs(sweepAngleDeg) >= 360f) return true
        val normalizedAngle = (angleDeg % 360f + 360f) % 360f
        val normalizedStart = (startAngleDeg % 360f + 360f) % 360f

        return if (sweepAngleDeg >= 0f) {
            val diff = (normalizedAngle - normalizedStart + 360f) % 360f
            diff <= sweepAngleDeg
        } else {
            val diff = (normalizedStart - normalizedAngle + 360f) % 360f
            diff <= abs(sweepAngleDeg)
        }
    }

    // =========================================================================
    // Window Enclosure Tests
    // =========================================================================

    fun isEntityEnclosed(entity: CadEntity, box: CadBoundingBox): Boolean {
        return when (entity) {
            is CadEntity.Point -> isPointInBox(entity.position, box)
            is CadEntity.Line -> isPointInBox(entity.start, box) && isPointInBox(entity.end, box)
            is CadEntity.Polyline -> entity.points.isNotEmpty() && entity.points.all { isPointInBox(it, box) }
            is CadEntity.Circle -> {
                entity.center.x - entity.radius >= box.minX &&
                entity.center.x + entity.radius <= box.maxX &&
                entity.center.y - entity.radius >= box.minY &&
                entity.center.y + entity.radius <= box.maxY
            }
            is CadEntity.Arc -> isBoundingBoxEnclosed(entity.boundingBox, box)
            is CadEntity.Text -> isBoundingBoxEnclosed(entity.boundingBox, box)
            is CadEntity.Dimension -> {
                isPointInBox(entity.start, box) &&
                isPointInBox(entity.end, box) &&
                isPointInBox(entity.textPoint, box)
            }
            is CadEntity.Leader -> {
                isPointInBox(entity.arrowPoint, box) &&
                isPointInBox(entity.kneePoint, box) &&
                isPointInBox(entity.landingEndPoint, box)
            }
            is CadEntity.Arrow -> {
                isPointInBox(entity.start, box) && isPointInBox(entity.end, box)
            }
            is CadEntity.RevisionCloud -> {
                entity.vertices.isNotEmpty() && entity.vertices.all { isPointInBox(it, box) }
            }
        }
    }

    fun isPointInBox(point: CadPoint2D, box: CadBoundingBox): Boolean {
        return point.x >= box.minX && point.x <= box.maxX &&
               point.y >= box.minY && point.y <= box.maxY
    }

    private fun isBoundingBoxEnclosed(inner: CadBoundingBox, outer: CadBoundingBox): Boolean {
        if (inner.isEmpty) return false
        return inner.minX >= outer.minX && inner.maxX <= outer.maxX &&
               inner.minY >= outer.minY && inner.maxY <= outer.maxY
    }

    // =========================================================================
    // Crossing Intersection Tests
    // =========================================================================

    fun isEntityCrossing(entity: CadEntity, box: CadBoundingBox): Boolean {
        // If completely enclosed, it satisfies crossing
        if (isEntityEnclosed(entity, box)) return true

        val left = box.minX
        val right = box.maxX
        val bottom = box.minY
        val top = box.maxY

        val p1 = CadPoint2D(left, bottom)
        val p2 = CadPoint2D(right, bottom)
        val p3 = CadPoint2D(right, top)
        val p4 = CadPoint2D(left, top)

        return when (entity) {
            is CadEntity.Point -> isPointInBox(entity.position, box)
            is CadEntity.Line -> {
                isPointInBox(entity.start, box) || isPointInBox(entity.end, box) ||
                segmentsIntersect(entity.start, entity.end, p1, p2) ||
                segmentsIntersect(entity.start, entity.end, p2, p3) ||
                segmentsIntersect(entity.start, entity.end, p3, p4) ||
                segmentsIntersect(entity.start, entity.end, p4, p1)
            }
            is CadEntity.Polyline -> {
                if (entity.points.any { isPointInBox(it, box) }) return true
                for (i in 0 until entity.points.size - 1) {
                    val a = entity.points[i]
                    val b = entity.points[i + 1]
                    if (segmentsIntersect(a, b, p1, p2) ||
                        segmentsIntersect(a, b, p2, p3) ||
                        segmentsIntersect(a, b, p3, p4) ||
                        segmentsIntersect(a, b, p4, p1)
                    ) return true
                }
                if (entity.isClosed && entity.points.size > 2) {
                    val a = entity.points.last()
                    val b = entity.points.first()
                    if (segmentsIntersect(a, b, p1, p2) ||
                        segmentsIntersect(a, b, p2, p3) ||
                        segmentsIntersect(a, b, p3, p4) ||
                        segmentsIntersect(a, b, p4, p1)
                    ) return true
                }
                false
            }
            is CadEntity.Circle -> {
                // Center inside box
                if (isPointInBox(entity.center, box)) return true
                // Any box edge intersects circle
                distancePointToSegment(entity.center, p1, p2) <= entity.radius ||
                distancePointToSegment(entity.center, p2, p3) <= entity.radius ||
                distancePointToSegment(entity.center, p3, p4) <= entity.radius ||
                distancePointToSegment(entity.center, p4, p1) <= entity.radius
            }
            is CadEntity.Arc -> {
                if (isPointInBox(entity.center, box)) return true
                entity.boundingBox.intersects(box)
            }
            is CadEntity.Text -> entity.boundingBox.intersects(box)
            is CadEntity.Dimension -> {
                isPointInBox(entity.start, box) || isPointInBox(entity.end, box) || isPointInBox(entity.textPoint, box) ||
                segmentsIntersect(entity.start, entity.end, p1, p2) ||
                segmentsIntersect(entity.start, entity.end, p2, p3) ||
                segmentsIntersect(entity.start, entity.end, p3, p4) ||
                segmentsIntersect(entity.start, entity.end, p4, p1)
            }
            is CadEntity.Leader -> {
                entity.boundingBox.intersects(box)
            }
            is CadEntity.Arrow -> {
                isPointInBox(entity.start, box) || isPointInBox(entity.end, box) ||
                segmentsIntersect(entity.start, entity.end, p1, p2) ||
                segmentsIntersect(entity.start, entity.end, p2, p3) ||
                segmentsIntersect(entity.start, entity.end, p3, p4) ||
                segmentsIntersect(entity.start, entity.end, p4, p1)
            }
            is CadEntity.RevisionCloud -> {
                entity.boundingBox.intersects(box)
            }
        }
    }

    /**
     * Determines whether two 2D line segments AB and CD intersect.
     */
    fun segmentsIntersect(a: CadPoint2D, b: CadPoint2D, c: CadPoint2D, d: CadPoint2D): Boolean {
        fun ccw(p1: CadPoint2D, p2: CadPoint2D, p3: CadPoint2D): Float {
            return (p2.x - p1.x) * (p3.y - p1.y) - (p2.y - p1.y) * (p3.x - p1.x)
        }

        val d1 = ccw(c, d, a)
        val d2 = ccw(c, d, b)
        val d3 = ccw(a, b, c)
        val d4 = ccw(a, b, d)

        if (((d1 > 0 && d2 < 0) || (d1 < 0 && d2 > 0)) &&
            ((d3 > 0 && d4 < 0) || (d3 < 0 && d4 > 0))
        ) return true

        fun onSegment(p: CadPoint2D, q: CadPoint2D, r: CadPoint2D): Boolean {
            return q.x <= max(p.x, r.x) && q.x >= min(p.x, r.x) &&
                   q.y <= max(p.y, r.y) && q.y >= min(p.y, r.y)
        }

        if (d1 == 0f && onSegment(c, a, d)) return true
        if (d2 == 0f && onSegment(c, b, d)) return true
        if (d3 == 0f && onSegment(a, c, b)) return true
        if (d4 == 0f && onSegment(a, d, b)) return true

        return false
    }
}
