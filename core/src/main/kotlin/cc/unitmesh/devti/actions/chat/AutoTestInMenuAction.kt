// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package cc.unitmesh.devti.actions.chat

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.gui.chat.message.ChatActionType
import cc.unitmesh.devti.intentions.action.task.TestCodeGenTask
import cc.unitmesh.devti.intentions.action.test.TestCodeGenRequest
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.ActionPlaces.PROJECT_VIEW_POPUP
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProgressManager
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
import java.util.concurrent.Executors

class AutoTestInMenuAction : AnAction(AutoDevBundle.message("intentions.chat.code.test.name")) {
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
        val total = files.size
        val executor = Executors.newSingleThreadExecutor()
        files.forEachIndexed { index, file ->
            val task = TestCodeGenTask(
                TestCodeGenRequest(file, file, project, editor),
                AutoDevBundle.message("intentions.chat.code.test.name")
            )

            executor.submit {
                val progressMessage = """${index + 1}/${total} Processing file ${file.name} for test generation"""
                ProgressManager.getInstance().runProcessWithProgressSynchronously(
                    {
                        task.run(object : EmptyProgressIndicator() {})
                    },
                    progressMessage, true, project
                )
            }
        }

        executor.shutdown()
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