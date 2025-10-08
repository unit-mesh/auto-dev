package cc.unitmesh.devti.diff.view

import com.intellij.diff.DiffContentFactory
import com.intellij.diff.DiffManager
import com.intellij.diff.contents.DiffContent
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vfs.VirtualFileManager
import javax.swing.SwingUtilities

@Service(Service.Level.PROJECT)
class DiffViewerService(private val project: Project) {

    private val logger = Logger.getInstance(DiffViewerService::class.java)

    fun openStandaloneCommitDiffPreview(change: Change, enableStaging: Boolean = true) {
        SwingUtilities.invokeLater {
            val standalonePreview = StandaloneEditorTabDiffPreview(project, change, enableStaging, logger)
            standalonePreview.performDiffAction()
        }
    }

    fun openStaticFileDiff(filePath: String, originalContent: String, modifiedContent: String) {
        val diffFactory = DiffContentFactory.getInstance()
        val fileType = FileTypeManager.getInstance().getFileTypeByFileName(filePath)
        val fileName = filePath.substringAfterLast('/')
        val virtualFile = VirtualFileManager.getInstance().findFileByUrl("file://$filePath")

        val originalDiffContent: DiffContent = if (virtualFile != null) {
            diffFactory.create(project, originalContent, virtualFile)
        } else {
            diffFactory.create(project, originalContent, fileType)
        }

        val modifiedDiffContent: DiffContent = if (virtualFile != null) {
            diffFactory.create(project, modifiedContent, virtualFile)
        } else {
            diffFactory.create(project, modifiedContent, fileType)
        }

        val diffRequest = SimpleDiffRequest(
            "Review Code Changes",
            originalDiffContent,
            modifiedDiffContent,
            "$fileName (Original)",
            "$fileName (Augmented)"
        )

        SwingUtilities.invokeLater {
            DiffManager.getInstance().showDiff(project, diffRequest)
        }
    }

    private fun checkFilesMatchIgnoringBom(content1: String, content2: String): Boolean {
        val content1NoBom = content1.removePrefix("\uFEFF")
        val content2NoBom = content2.removePrefix("\uFEFF")
        return content1NoBom.length == content2NoBom.length && content1NoBom == content2NoBom
    }

    companion object {
        fun getInstance(project: Project): DiffViewerService = project.service()
    }
}