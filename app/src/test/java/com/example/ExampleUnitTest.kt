package com.example

import com.example.cad.engine.DefaultCadEditEngine
import com.example.cad.engine.DefaultCadEngine
import com.example.cad.engine.DefaultCadMeasurementEngine
import com.example.cad.engine.MeasurementResult
import com.example.cad.model.CadBoundingBox
import com.example.cad.model.CadEntity
import com.example.cad.model.CadFormat
import com.example.cad.model.CadPoint2D
import com.example.cad.model.CadUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

  @Test
  fun cadMeasurementEngine_calculatesEuclideanDistance() {
    val engine = DefaultCadMeasurementEngine()
    val p1 = CadPoint2D(0f, 0f)
    val p2 = CadPoint2D(3000f, 4000f) // 3-4-5 right triangle

    val result = engine.measureDistance(p1, p2, CadUnit.MILLIMETERS)
    assertEquals(5000.0, result.value, 0.001)
    assertEquals(3000.0, result.deltaX, 0.001)
    assertEquals(4000.0, result.deltaY, 0.001)
  }

  @Test
  fun cadMeasurementEngine_calculatesPolygonAreaShoelace() {
    val engine = DefaultCadMeasurementEngine()
    // 1000mm x 2000mm rectangle -> area 2,000,000 mm^2
    val points = listOf(
      CadPoint2D(0f, 0f),
      CadPoint2D(1000f, 0f),
      CadPoint2D(1000f, 2000f),
      CadPoint2D(0f, 2000f)
    )

    val result = engine.measurePolygonArea(points, CadUnit.MILLIMETERS)
    assertEquals(2_000_000.0, result.valueSquareUnits, 0.001)
    assertEquals(6000.0, result.perimeter, 0.001)
  }

  @Test
  fun cadEditEngine_handlesUndoRedoCommands() {
    val editEngine = DefaultCadEditEngine()
    val cadEngine = DefaultCadEngine()
    val initialDoc = cadEngine.createEmptyDocument("Test.dxf")

    assertFalse(editEngine.canUndo())
    assertFalse(editEngine.canRedo())

    val line = CadEntity.Line("line_1", "0", null, CadPoint2D(0f, 0f), CadPoint2D(100f, 100f))
    val docWithLine = editEngine.addEntity(initialDoc, line)

    assertEquals(1, docWithLine.entities.size)
    assertTrue(editEngine.canUndo())

    val undoneDoc = editEngine.undo(docWithLine)
    assertEquals(0, undoneDoc.entities.size)
    assertTrue(editEngine.canRedo())

    val redoneDoc = editEngine.redo(undoneDoc)
    assertEquals(1, redoneDoc.entities.size)
  }

  @Test
  fun cadFormat_recognizesExtensions() {
    assertEquals(CadFormat.DWG, CadFormat.fromFilename("floorplan.dwg"))
    assertEquals(CadFormat.DXF, CadFormat.fromFilename("assembly.dxf"))
    assertEquals(CadFormat.STEP, CadFormat.fromFilename("gear.step"))
  }
}
