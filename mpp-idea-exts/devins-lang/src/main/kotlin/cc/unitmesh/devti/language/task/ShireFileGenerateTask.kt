package cc.unitmesh.devti.language.task

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.devins.PostFunction
import cc.unitmesh.devti.llms.LlmFactory
import cc.unitmesh.devti.util.parser.CodeFence
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.runBlocking
import java.nio.file.Path
import kotlin.io.path.Path

open class ShireFileGenerateTask(
    @JvmField val project: Project,
    val prompt: String,
    val fileName: String?,
    private val codeOnly: Boolean = false,
    private val taskName: String = AutoDevBundle.message("intentions.request.background.process.title"),
    postExecute: PostFunction,
) : ShireInteractionTask(project, taskName, postExecute) {
    private val projectRoot = project.guessProjectDir()!!

    override fun run(indicator: ProgressIndicator) {
        val stream = LlmFactory.create(project)?.stream(prompt, "", false)
        if (stream == null) {
            logger<ShireFileGenerateTask>().error("Failed to create stream")
            postExecute?.invoke("", null)
            return
        }

        var result = ""
        runBlocking {
            stream.cancellable().collect {
                result += it
            }
        }

        val inferFileName = if (fileName == null) {
            val language = CodeFence.parse(result).language
            val timestamp = System.currentTimeMillis()
            "output-" + timestamp + if (language == PlainTextLanguage.INSTANCE) ".txt" else ".$language"
        } else {
            fileName
        }

        val file = project.guessProjectDir()?.toNioPath()?.resolve(inferFileName)?.toFile()
        if (file == null) {
            logger<ShireFileGenerateTask>().error("Failed to create file")
            postExecute?.invoke(result, null)
            return
        }
        if (!file.exists()) {
            file.createNewFile()
        }

        if (codeOnly) {
            val code = CodeFence.parse(result).text
            file.writeText(code)
            refreshAndOpenInEditor(file.toPath(), projectRoot)
        } else {
            file.writeText(result)
            refreshAndOpenInEditor(Path(projectRoot.path), projectRoot)
        }

        postExecute?.invoke(result, null)
    }

    private fun refreshAndOpenInEditor(file: Path, parentDir: VirtualFile) = runBlocking {
        ProgressManager.getInstance().run(RefreshProjectModal(file, parentDir))
    }

    inner class RefreshProjectModal(private val file: Path, private val parentDir: VirtualFile) :
        Modal(project, "Refreshing Project Model", true) {
        override fun run(indicator: ProgressIndicator) {
            repeat(5) {
                val virtualFile = LocalFileSystem.getInstance().findFileByNioFile(file)
                if (virtualFile == null) {
                    VfsUtil.markDirtyAndRefresh(true, true, true, parentDir)
                } else {
                    try {
                        runInEdt {
                            FileEditorManager.getInstance(project).openFile(virtualFile, true)
                        }
                        return
                    } catch (e: Exception) {
                        //
                    }
                }
            }
        }
    }
}