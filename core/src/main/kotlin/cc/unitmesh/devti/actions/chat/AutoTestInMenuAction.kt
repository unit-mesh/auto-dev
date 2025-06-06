// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package cc.unitmesh.devti.actions.chat

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.gui.chat.message.ChatActionType
import cc.unitmesh.devti.intentions.action.task.TestCodeGenTask
import cc.unitmesh.devti.intentions.action.test.TestCodeGenRequest
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.ActionPlaces.PROJECT_VIEW_POPUP
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.*
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileVisitor
import com.intellij.openapi.wm.WindowManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.file.PsiDirectoryFactory
import com.intellij.openapi.diagnostic.logger

class AutoTestInMenuAction : AnAction(AutoDevBundle.message("intentions.chat.code.test.name")) {
    private val logger = logger<AutoTestInMenuAction>()

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    fun getActionType(): ChatActionType = ChatActionType.GENERATE_TEST

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = isEnabled(e)
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val files = getSelectedWritableFiles(e)
        val editor = e.getData(CommonDataKeys.EDITOR)

        if (files.isEmpty()) {
            showNothingToConvertErrorMessage(project)
            return
        }

        if (files.size == 1) {
            val file = files[0]

            val task = TestCodeGenTask(
                TestCodeGenRequest(file, file, project, editor),
                AutoDevBundle.message("intentions.chat.code.test.name")
            )

            ProgressManager.getInstance()
                .runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))

            return
        }

        batchGenerateTests(files, project, editor)
    }

    private fun batchGenerateTests(files: List<PsiFile>, project: Project, editor: Editor?) {
        val batchTask = object : Task.Backgroundable(
            project,
            AutoDevBundle.message("intentions.chat.code.test.name") + " (Batch)",
            true
        ) {
            override fun run(indicator: ProgressIndicator) {
                val total = files.size
                indicator.isIndeterminate = false
                indicator.fraction = 0.0

                files.forEachIndexed { index, file ->
                    // Check for cancellation before processing each file
                    indicator.checkCanceled()

                    indicator.text = "Processing ${index + 1}/$total: ${file.name}"
                    indicator.fraction = index.toDouble() / total

                    val task = TestCodeGenTask(
                        TestCodeGenRequest(file, file, project, editor),
                        AutoDevBundle.message("intentions.chat.code.test.name")
                    )

                    try {
                        task.run(indicator)
                        indicator.fraction = (index + 1).toDouble() / total
                    } catch (e: ProcessCanceledException) {
                        // User cancelled, stop processing
                        indicator.text = "Batch test generation cancelled"
                        throw e
                    } catch (e: Exception) {
                        // Log error but continue with next file
                        logger.warn("Failed to generate test for file: ${file.name}", e)
                        indicator.fraction = (index + 1).toDouble() / total
                    }
                }

                indicator.fraction = 1.0
                indicator.text = "Batch test generation completed"
            }
        }

        ProgressManager.getInstance()
            .runProcessWithProgressAsynchronously(batchTask, BackgroundableProcessIndicator(batchTask))
    }

    private fun isEnabled(e: AnActionEvent): Boolean {
        val project = e.project ?: return false
        if (project.isDisposed) return false
        if (e.getData(PlatformCoreDataKeys.MODULE) == null) return false
        val files = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) ?: return false

        fun isWritableJavaFile(file: VirtualFile): Boolean {
            return file.isWritable
        }

        fun isWritablePackageDirectory(file: VirtualFile): Boolean {
            val directory = PsiManager.getInstance(project).findDirectory(file) ?: return false
            return PsiDirectoryFactory.getInstance(project).isPackage(directory) && file.isWritable
        }

        if (e.place != PROJECT_VIEW_POPUP && files.any(::isWritablePackageDirectory)) {
            return true
        }

        return files.any(::isWritableJavaFile)
    }

    private fun showNothingToConvertErrorMessage(project: Project) {
        val statusBar = WindowManager.getInstance().getStatusBar(project)
        JBPopupFactory.getInstance()
            .createHtmlTextBalloonBuilder(AutoDevBundle.message("batch.nothing.to.testing"), MessageType.ERROR, null)
            .createBalloon()
            .showInCenterOf(statusBar.component)
    }

    private fun getSelectedWritableFiles(e: AnActionEvent): List<PsiFile> {
        val virtualFilesAndDirectories = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) ?: return emptyList()
        val project = e.project ?: return emptyList()
        val psiManager = PsiManager.getInstance(project)
        return getAllFilesRecursively(virtualFilesAndDirectories)
            .asSequence()
            .mapNotNull { psiManager.findFile(it) }
            .filter { it.isWritable }
            .toList()
    }
}

fun getAllFilesRecursively(filesOrDirs: Array<VirtualFile>): Collection<VirtualFile> {
    val result = ArrayList<VirtualFile>()
    for (file in filesOrDirs) {
        VfsUtilCore.visitChildrenRecursively(file, object : VirtualFileVisitor<Unit>() {
            override fun visitFile(file: VirtualFile): Boolean {
                result.add(file)
                return true
            }
        })
    }
    return result
}