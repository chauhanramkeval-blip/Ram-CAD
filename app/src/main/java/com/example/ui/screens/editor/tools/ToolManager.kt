package com.example.ui.screens.editor.tools

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Manages active UI tool state, popup panel visibility, pinned/favorite tools,
 * and command dispatch coordination.
 */
class ToolManager(
    initialCategory: PopupCategory? = null,
    initialPinnedIds: Set<String> = setOf("draw_line", "draw_polyline", "edit_select", "measure_distance")
) {
    /** The currently open popup category, or null if all popups are closed */
    var activeCategory by mutableStateOf<PopupCategory?>(initialCategory)
        private set

    /** The ID of the currently selected/active tool */
    var activeToolId by mutableStateOf<String?>("draw_line")
        private set

    /** Set of pinned favorite tool IDs */
    var pinnedToolIds by mutableStateOf<Set<String>>(initialPinnedIds)
        private set

    /** Whether the popup panel should remain open after selecting a command */
    var isKeepPopupOpen by mutableStateOf(false)
        private set

    /**
     * Toggles a category popup open or closed.
     */
    fun toggleCategory(category: PopupCategory) {
        activeCategory = if (activeCategory == category) null else category
    }

    /**
     * Explicitly opens a category popup.
     */
    fun openCategory(category: PopupCategory) {
        activeCategory = category
    }

    /**
     * Closes the active popup panel.
     */
    fun closePopup() {
        activeCategory = null
    }

    /**
     * Toggles the pin/favorite state of a tool.
     */
    fun togglePinTool(toolId: String) {
        pinnedToolIds = if (pinnedToolIds.contains(toolId)) {
            pinnedToolIds - toolId
        } else {
            pinnedToolIds + toolId
        }
    }

    /**
     * Returns true if a tool is pinned/favorited.
     */
    fun isToolPinned(toolId: String): Boolean = pinnedToolIds.contains(toolId)

    /**
     * Returns the list of currently pinned tools.
     */
    fun getPinnedTools(): List<ToolDefinition> {
        return pinnedToolIds.mapNotNull { ToolRegistry.getToolById(it) }
    }

    /**
     * Toggles the keep-open / auto-close preference for popups.
     */
    fun toggleKeepOpen() {
        isKeepPopupOpen = !isKeepPopupOpen
    }

    /**
     * Sets the active tool ID directly (e.g. when canvas tool changes).
     */
    fun setActiveTool(toolId: String) {
        activeToolId = toolId
    }

    /**
     * Executes a tool via the provided [CommandManager].
     * Closes the popup if the command executed successfully and [isKeepPopupOpen] is false.
     */
    fun executeTool(tool: ToolDefinition, commandManager: CommandManager) {
        activeToolId = tool.id
        val canClose = commandManager.execute(tool)
        if (canClose && !isKeepPopupOpen) {
            closePopup()
        }
    }
}
