package com.example.cad.engine.history

import com.example.cad.model.CadDocument

/**
 * Represents an executable and reversible CAD operation within the command history system.
 *
 * Implements the Command Pattern for CAD documents:
 * - [execute] applies the forward transformation on the document.
 * - [undo] reverses the transformation, returning the document to its prior consistent state.
 * - [redo] reapplies the transformation.
 * - [name] provides a user-readable description of the operation for the command line and history inspect UI.
 * - [estimatedMemoryBytes] gives an approximate memory footprint for memory-safe stack management.
 */
interface CadCommand {
    val name: String
    val estimatedMemoryBytes: Int
        get() = 256

    /**
     * Executes or reapplies the command on the given [document].
     */
    fun execute(document: CadDocument): CadDocument

    /**
     * Undoes the command on the given [document], returning the document to its state before [execute].
     */
    fun undo(document: CadDocument): CadDocument

    /**
     * Re-applies the command (default calls execute).
     */
    fun redo(document: CadDocument): CadDocument = execute(document)
}
