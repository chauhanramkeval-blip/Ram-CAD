package com.example.cad.engine.history

import com.example.cad.model.CadDocument
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * State representing the status of the Command History stack.
 */
data class CadCommandHistoryState(
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val undoCount: Int = 0,
    val redoCount: Int = 0,
    val lastCommandName: String? = null,
    val nextRedoCommandName: String? = null,
    val undoStackNames: List<String> = emptyList(),
    val redoStackNames: List<String> = emptyList(),
    val estimatedMemoryBytes: Long = 0L,
    val isRecordingGroup: Boolean = false
)

/**
 * Professional CAD Command History Manager.
 *
 * Implements:
 * 1. Unlimited reasonable history (default capacity 500 commands, configurable or dynamic based on memory).
 * 2. Memory-safe implementation (pruning oldest commands if total memory or stack size exceeds bounds).
 * 3. Group related operations into one undo command (via [beginCommandGroup] and [commitCommandGroup]).
 * 4. Undo/Redo state updates automatically (via [StateFlow] and callbacks).
 * 5. Guarantees CAD state remains consistent using invertible commands with exact entity preservation.
 */
class CadCommandHistory(
    val maxHistorySize: Int = 500,
    val maxMemoryBytes: Long = 10 * 1024 * 1024 // 10 MB limit for command history
) {
    private val undoStack = ArrayDeque<CadCommand>()
    private val redoStack = ArrayDeque<CadCommand>()

    // For grouping related commands into an atomic undo step
    private var activeGroupCommands: MutableList<CadCommand>? = null
    private var activeGroupName: String? = null

    private val _historyState = MutableStateFlow(CadCommandHistoryState())
    val historyState: StateFlow<CadCommandHistoryState> = _historyState.asStateFlow()

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()
    val undoCount: Int get() = undoStack.size
    val redoCount: Int get() = redoStack.size

    val lastCommandName: String? get() = undoStack.lastOrNull()?.name
    val nextRedoCommandName: String? get() = redoStack.lastOrNull()?.name

    /**
     * Executes a command on the document and records it in history.
     * If a command group is active, it records into the group instead of pushing directly.
     */
    fun executeCommand(document: CadDocument, command: CadCommand): CadDocument {
        val newDoc = command.execute(document)

        val group = activeGroupCommands
        if (group != null) {
            group.add(command)
            updateState()
            return newDoc
        }

        undoStack.addLast(command)
        redoStack.clear()

        enforceMemorySafety()
        updateState()
        return newDoc
    }

    /**
     * Undoes the top command on the undo stack.
     */
    fun undo(document: CadDocument): CadDocument {
        if (!canUndo) return document
        val command = undoStack.removeLast()
        val newDoc = command.undo(document)
        redoStack.addLast(command)
        updateState()
        return newDoc
    }

    /**
     * Redoes the top command on the redo stack.
     */
    fun redo(document: CadDocument): CadDocument {
        if (!canRedo) return document
        val command = redoStack.removeLast()
        val newDoc = command.redo(document)
        undoStack.addLast(command)
        updateState()
        return newDoc
    }

    /**
     * Begins grouping subsequent commands into a single atomic undo/redo command.
     */
    fun beginCommandGroup(groupName: String? = null) {
        if (activeGroupCommands == null) {
            activeGroupCommands = mutableListOf()
            activeGroupName = groupName
            updateState()
        }
    }

    /**
     * Commits the active group of commands as a single [CompositeCadCommand].
     * If only 1 command was recorded, stores that command directly.
     * If no commands were recorded, does nothing.
     */
    fun commitCommandGroup(groupName: String? = null) {
        val group = activeGroupCommands ?: return
        activeGroupCommands = null
        val name = groupName ?: activeGroupName ?: "Grouped Edit"
        activeGroupName = null

        if (group.isNotEmpty()) {
            val composite = if (group.size == 1) {
                group[0]
            } else {
                CompositeCadCommand(group, name = name)
            }
            undoStack.addLast(composite)
            redoStack.clear()
            enforceMemorySafety()
        }
        updateState()
    }

    /**
     * Cancels the active command group without recording it to the undo stack.
     * Note: any document mutations already executed should be undone by the caller.
     */
    fun cancelCommandGroup() {
        activeGroupCommands = null
        activeGroupName = null
        updateState()
    }

    /**
     * Clears all undo and redo history.
     */
    fun clearHistory() {
        undoStack.clear()
        redoStack.clear()
        activeGroupCommands = null
        activeGroupName = null
        updateState()
    }

    /**
     * Enforces stack capacity and memory bounds.
     */
    private fun enforceMemorySafety() {
        // Enforce max count
        while (undoStack.size > maxHistorySize) {
            undoStack.removeFirst()
        }

        // Enforce estimated memory bounds
        var totalBytes = calculateTotalMemoryBytes()
        while (totalBytes > maxMemoryBytes && undoStack.isNotEmpty()) {
            val removed = undoStack.removeFirst()
            totalBytes -= removed.estimatedMemoryBytes
        }
    }

    private fun calculateTotalMemoryBytes(): Long {
        var sum = 0L
        for (cmd in undoStack) sum += cmd.estimatedMemoryBytes
        for (cmd in redoStack) sum += cmd.estimatedMemoryBytes
        activeGroupCommands?.let {
            for (cmd in it) sum += cmd.estimatedMemoryBytes
        }
        return sum
    }

    private fun updateState() {
        _historyState.value = CadCommandHistoryState(
            canUndo = canUndo,
            canRedo = canRedo,
            undoCount = undoCount,
            redoCount = redoCount,
            lastCommandName = lastCommandName,
            nextRedoCommandName = nextRedoCommandName,
            undoStackNames = undoStack.map { it.name },
            redoStackNames = redoStack.map { it.name },
            estimatedMemoryBytes = calculateTotalMemoryBytes(),
            isRecordingGroup = activeGroupCommands != null
        )
    }
}
