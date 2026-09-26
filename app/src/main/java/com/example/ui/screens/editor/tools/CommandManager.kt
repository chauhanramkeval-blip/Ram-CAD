package com.example.ui.screens.editor.tools

import com.example.cad.engine.edit.EditOperationType
import com.example.ui.screens.editor.CadTool

/**
 * Interface through which commands interact with the CAD environment,
 * keeping the UI completely independent from CAD engine internals.
 */
interface CadActionDispatcher {
    val hasSelection: Boolean
    val selectedCount: Int
    val activeTool: CadTool

    fun selectCadTool(tool: CadTool)
    fun startEditCommand(operation: EditOperationType)
    fun deleteSelectedEntities()
    fun openLayersSheet()
    fun createNewLayer(name: String, colorArgb: Long)
    fun deleteCurrentLayer()
    fun cycleCurrentLayer()
    fun toggleCurrentLayerVisibility()
    fun toggleCurrentLayerLock()
    fun changeCurrentLayerColor(colorArgb: Long)
    fun setSelectionColor(colorArgb: Long?)
    fun openPropertiesSheet()
    fun toggleGrid()
    fun toggleSnap()
    fun openOsnapSheet()
    fun toggleOrtho()
    fun toggleCoordinates()
    fun cycleUnits()
    fun cyclePrecision()
    fun toggleLineweights()
    fun cycleCanvasBackground()
    fun openMeasurementSheet()
    fun showStatusMessage(message: String)
}

/**
 * Central Command Manager responsible for executing tool commands,
 * enforcing selection prerequisites, and handling engine-dependent tools cleanly.
 */
class CommandManager(private val dispatcher: CadActionDispatcher) {

    /**
     * Executes a tool command.
     * @return true if the tool was executed and the popup can be closed (if auto-close is enabled).
     */
    fun execute(tool: ToolDefinition): Boolean {
        // 1. Check if tool requires external engine
        if (tool.availability == ToolAvailability.ENGINE_REQUIRED) {
            dispatcher.showStatusMessage("[Engine Required] ${tool.name}: ${tool.description}")
            return false
        }

        // 2. Check if tool is marked coming soon
        if (tool.availability == ToolAvailability.COMING_SOON) {
            dispatcher.showStatusMessage("[Coming Soon] ${tool.name}: ${tool.description}")
            return false
        }

        // 3. Check selection prerequisites
        if (tool.requiresSelection && !dispatcher.hasSelection) {
            dispatcher.showStatusMessage("Selection required: Select one or more objects first to use ${tool.name}")
            // Switch to select tool so the user can easily select
            dispatcher.selectCadTool(CadTool.SELECT)
            return false
        }

        // 4. Dispatch the command
        return when (tool.command) {
            // --- ANNOTATION COMMANDS ---
            "CMD_ANNOT_TEXT" -> {
                dispatcher.selectCadTool(CadTool.TEXT)
                true
            }
            "CMD_ANNOT_MTEXT" -> {
                dispatcher.selectCadTool(CadTool.MTEXT)
                true
            }
            "CMD_ANNOT_LEADER" -> {
                dispatcher.selectCadTool(CadTool.LEADER)
                true
            }
            "CMD_ANNOT_MULTILEADER" -> {
                dispatcher.selectCadTool(CadTool.LEADER)
                true
            }
            "CMD_ANNOT_ARROW" -> {
                dispatcher.selectCadTool(CadTool.ARROW)
                true
            }
            "CMD_ANNOT_CLOUD" -> {
                dispatcher.selectCadTool(CadTool.CLOUD)
                true
            }

            // --- DRAW COMMANDS ---
            "CMD_DRAW_POLYLINE" -> {
                dispatcher.selectCadTool(CadTool.POLYLINE)
                true
            }
            "CMD_DRAW_LINE" -> {
                dispatcher.selectCadTool(CadTool.LINE)
                true
            }
            "CMD_DRAW_TEXT" -> {
                dispatcher.selectCadTool(CadTool.TEXT)
                true
            }
            "CMD_DRAW_CIRCLE" -> {
                dispatcher.selectCadTool(CadTool.CIRCLE)
                true
            }
            "CMD_DRAW_ARC" -> {
                dispatcher.selectCadTool(CadTool.ARC)
                true
            }
            "CMD_DRAW_RECTANGLE" -> {
                dispatcher.selectCadTool(CadTool.RECTANGLE)
                true
            }
            "CMD_DRAW_MULTILEADER" -> {
                dispatcher.selectCadTool(CadTool.LEADER)
                true
            }
            "CMD_DRAW_CLOUD" -> {
                dispatcher.selectCadTool(CadTool.CLOUD)
                true
            }

            // --- EDIT COMMANDS ---
            "CMD_EDIT_SELECT" -> {
                dispatcher.selectCadTool(CadTool.SELECT)
                true
            }
            "CMD_EDIT_MOVE" -> {
                dispatcher.startEditCommand(EditOperationType.MOVE)
                true
            }
            "CMD_EDIT_COPY" -> {
                dispatcher.startEditCommand(EditOperationType.COPY)
                true
            }
            "CMD_EDIT_ROTATE" -> {
                dispatcher.startEditCommand(EditOperationType.ROTATE)
                true
            }
            "CMD_EDIT_SCALE" -> {
                dispatcher.startEditCommand(EditOperationType.SCALE)
                true
            }
            "CMD_EDIT_TRIM" -> {
                dispatcher.selectCadTool(CadTool.TRIM)
                true
            }
            "CMD_EDIT_EXTEND" -> {
                dispatcher.selectCadTool(CadTool.EXTEND)
                true
            }
            "CMD_EDIT_ERASE" -> {
                if (dispatcher.hasSelection) {
                    dispatcher.deleteSelectedEntities()
                } else {
                    dispatcher.selectCadTool(CadTool.ERASER)
                }
                true
            }

            // --- LAYER COMMANDS ---
            "CMD_LAYER_LIST" -> {
                dispatcher.openLayersSheet()
                true
            }
            "CMD_LAYER_NEW" -> {
                dispatcher.createNewLayer("Layer_${System.currentTimeMillis() % 1000}", 0xFF00E5FF)
                true
            }
            "CMD_LAYER_DELETE" -> {
                dispatcher.deleteCurrentLayer()
                true
            }
            "CMD_LAYER_CURRENT" -> {
                dispatcher.cycleCurrentLayer()
                false // keep popup open to see the change
            }
            "CMD_LAYER_VISIBILITY" -> {
                dispatcher.toggleCurrentLayerVisibility()
                false
            }
            "CMD_LAYER_LOCK" -> {
                dispatcher.toggleCurrentLayerLock()
                false
            }
            "CMD_LAYER_COLOR" -> {
                // Cycle layer color
                dispatcher.changeCurrentLayerColor(0xFFFFD600)
                false
            }

            // --- MEASURE COMMANDS ---
            "CMD_MEASURE_DISTANCE" -> {
                dispatcher.selectCadTool(CadTool.MEASURE_DISTANCE)
                true
            }
            "CMD_MEASURE_CONTINUOUS" -> {
                dispatcher.selectCadTool(CadTool.MEASURE_POLYLINE)
                true
            }
            "CMD_MEASURE_BATCH" -> {
                dispatcher.selectCadTool(CadTool.MEASURE_SELECTION)
                true
            }
            "CMD_MEASURE_AREA" -> {
                dispatcher.selectCadTool(CadTool.MEASURE_AREA)
                true
            }
            "CMD_MEASURE_ID_POINT" -> {
                dispatcher.selectCadTool(CadTool.MEASURE_COORDINATE)
                true
            }
            "CMD_MEASURE_ARC_LENGTH" -> {
                dispatcher.selectCadTool(CadTool.MEASURE_PERIMETER)
                true
            }
            "CMD_MEASURE_ENTITY" -> {
                dispatcher.selectCadTool(CadTool.MEASURE_SELECTION)
                true
            }
            "CMD_MEASURE_ANGLE" -> {
                dispatcher.selectCadTool(CadTool.MEASURE_ANGLE)
                true
            }
            "CMD_MEASURE_RESULT" -> {
                dispatcher.openMeasurementSheet()
                true
            }
            "CMD_MEASURE_RESULT_COUNT" -> {
                dispatcher.openMeasurementSheet()
                true
            }
            "CMD_MEASURE_PRECISION" -> {
                dispatcher.cyclePrecision()
                false
            }

            // --- DIMENSION COMMANDS ---
            "CMD_DIM_LINEAR" -> {
                dispatcher.selectCadTool(CadTool.MEASURE_HORIZONTAL)
                true
            }
            "CMD_DIM_ALIGNED" -> {
                dispatcher.selectCadTool(CadTool.MEASURE_ALIGNED)
                true
            }
            "CMD_DIM_ANGULAR" -> {
                dispatcher.selectCadTool(CadTool.MEASURE_ANGLE)
                true
            }
            "CMD_DIM_RADIUS" -> {
                dispatcher.selectCadTool(CadTool.MEASURE_RADIUS)
                true
            }
            "CMD_DIM_DIAMETER" -> {
                dispatcher.selectCadTool(CadTool.MEASURE_DIAMETER)
                true
            }

            // --- COLOR COMMANDS ---
            "CMD_COLOR_BYLAYER" -> {
                dispatcher.setSelectionColor(null)
                dispatcher.showStatusMessage("Color set to ByLayer")
                true
            }
            "CMD_COLOR_BYBLOCK" -> {
                dispatcher.setSelectionColor(0xFFFFFFFF)
                dispatcher.showStatusMessage("Color set to ByBlock")
                true
            }
            "CMD_COLOR_OBJECT" -> {
                dispatcher.setSelectionColor(0xFF00E5FF) // Electric Cyan
                dispatcher.showStatusMessage("Assigned CAD Electric Cyan")
                false
            }
            "CMD_COLOR_CUSTOM" -> {
                dispatcher.setSelectionColor(0xFFFFD600) // CAD Dimension Yellow
                dispatcher.showStatusMessage("Assigned CAD Dimension Yellow")
                false
            }

            // --- TOOL COMMANDS ---
            "CMD_TOOL_OSNAP" -> {
                dispatcher.openOsnapSheet()
                true
            }
            "CMD_TOOL_GRID" -> {
                dispatcher.toggleGrid()
                false
            }
            "CMD_TOOL_ORTHO" -> {
                dispatcher.toggleOrtho()
                false
            }
            "CMD_TOOL_SNAP" -> {
                dispatcher.toggleSnap()
                false
            }
            "CMD_TOOL_COORDINATES" -> {
                dispatcher.toggleCoordinates()
                false
            }
            "CMD_TOOL_UNITS" -> {
                dispatcher.cycleUnits()
                false
            }
            "CMD_TOOL_PRECISION" -> {
                dispatcher.cyclePrecision()
                false
            }
            "CMD_TOOL_PROPERTIES" -> {
                dispatcher.openPropertiesSheet()
                true
            }

            // --- LAYOUT COMMANDS ---
            "CMD_LAYOUT_MODEL" -> {
                dispatcher.showStatusMessage("Active: Model Space (Infinite 2D CAD Canvas)")
                true
            }

            // --- VISUAL STYLE COMMANDS ---
            "CMD_VS_WIREFRAME" -> {
                dispatcher.showStatusMessage("Visual Style: 2D Vector Wireframe")
                true
            }
            "CMD_VS_LINEWEIGHT" -> {
                dispatcher.toggleLineweights()
                false
            }
            "CMD_VS_BACKGROUND" -> {
                dispatcher.cycleCanvasBackground()
                false
            }
            "CMD_VS_GRID" -> {
                dispatcher.toggleGrid()
                false
            }

            else -> {
                dispatcher.showStatusMessage("Executing command: ${tool.name}")
                true
            }
        }
    }
}
