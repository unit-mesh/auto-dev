package com.intellij.temporary.inlay.codecomplete

import com.intellij.openapi.command.CommandEvent
import com.intellij.openapi.command.CommandListener
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.VisualPosition
import com.intellij.openapi.editor.ex.DocumentEx
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

private class UndoTransparentActionState(val editor: Editor, val modificationStamp: Long)
private class CommandEditorState(val modificationStamp: Long, val visualPosition: VisualPosition)
class LLMCommandListener(private val project: Project) : CommandListener {
    private val activeCommands = AtomicInteger()
    private val startedWithEditor = AtomicBoolean(false)
    private val undoTransparentActionStamp = AtomicReference<UndoTransparentActionState?>()

    override fun commandStarted(event: CommandEvent) {
        if (activeCommands.getAndIncrement() > 0) {
            logger.debug("Skipping nested commandStarted. Event: $event")
            return
        }

        val editor = getSelectedEditorSafely(project)
        if (editor != null) {
            startedWithEditor.set(true)
            COMMAND_STATE_KEY[editor] = createCommandState(editor)
        } else {
            startedWithEditor.set(false)
        }
    }


    override fun commandFinished(event: CommandEvent) {
        if (activeCommands.decrementAndGet() > 0) {
            logger.debug("Skipping nested commandFinished. Event: $event")
            return
        }

        if (!startedWithEditor.get()) return

        val editor = getSelectedEditorSafely(project) ?: return
        val editorManager = LLMInlayManager.getInstance()

        if (!editorManager.isAvailable(editor)) return

        val commandStartState = COMMAND_STATE_KEY[editor] ?: return
        val commandEndState = createCommandState(editor)
        if (isDocumentModification(commandStartState, commandEndState)) {
            logger.debug("command modified document: " + event.commandName)
            editorManager.editorModified(editor)
        } else if (isCaretPositionChange(commandStartState, commandEndState)) {
            editorManager.disposeInlays(editor, InlayDisposeContext.CaretChange)
        }
    }

    override fun undoTransparentActionStarted() {
        val editor = getSelectedEditorSafely(project)
        undoTransparentActionStamp.set(if (editor != null) createUndoTransparentState(editor) else null)
    }

    override fun undoTransparentActionFinished() {
        val currentEditorStamp = undoTransparentActionStamp.get()
        undoTransparentActionStamp.set(null)
        val editor = getSelectedEditorSafely(project) ?: return

        if (currentEditorStamp == null || editor !== currentEditorStamp.editor) return
        if (getDocumentStamp(editor.document) == currentEditorStamp.modificationStamp) return

        val editorManager = LLMInlayManager.getInstance()
        if (editorManager.isAvailable(editor)) {
            editorManager.editorModified(editor)
        }
    }

    private fun getSelectedEditorSafely(project: Project): Editor? {
        return try {
            FileEditorManager.getInstance(project)?.selectedTextEditor
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        private val logger = logger<LLMCommandListener>()
        private val COMMAND_STATE_KEY = Key.create<CommandEditorState>("llm.commandState")
    }

    private fun createCommandState(editor: Editor): CommandEditorState {
        return CommandEditorState(getDocumentStamp(editor.document), editor.caretModel.visualPosition)
    }

    private fun createUndoTransparentState(editor: Editor): UndoTransparentActionState {
        return UndoTransparentActionState(editor, getDocumentStamp(editor.document))
    }

    private fun getDocumentStamp(document: Document): Long {
        return if (document is DocumentEx) document.modificationSequence.toLong() else document.modificationStamp
    }

    private fun isDocumentModification(first: CommandEditorState, second: CommandEditorState): Boolean {
        return first.modificationStamp != second.modificationStamp
    }

    private fun isCaretPositionChange(first: CommandEditorState, second: CommandEditorState): Boolean {
        return first.visualPosition != second.visualPosition
    }
}