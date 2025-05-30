package cc.unitmesh.devti.util

import cc.unitmesh.devti.observer.agent.AgentStateService
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.application.ReadAction.*
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.diff.impl.patch.TextFilePatch
import com.intellij.openapi.diff.impl.patch.apply.GenericPatchApplier
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ThrowableComputable
import com.intellij.openapi.vcs.FilePath
import com.intellij.openapi.vcs.FileStatus
import com.intellij.openapi.vcs.VcsBundle
import com.intellij.openapi.vcs.VcsException
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.ContentRevision
import com.intellij.openapi.vcs.changes.CurrentContentRevision
import com.intellij.openapi.vcs.changes.TextRevisionNumber
import com.intellij.openapi.vcs.history.VcsRevisionNumber
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.vcsUtil.VcsUtil
import org.jetbrains.annotations.NonNls
import java.io.File
import java.io.IOException

object PatchConverter {
    fun getAbsolutePath(baseDir: File, relativePath: String): File {
        var file: File?
        try {
            file = File(baseDir, relativePath).getCanonicalFile()
        } catch (e: IOException) {
            logger<PatchConverter>().info(e)
            file = File(baseDir, relativePath)
        }

        return file
    }

    fun createChange(project: Project, patch: TextFilePatch): Change {
        val baseDir = File(project.basePath!!)
        val beforePath = patch.beforeName
        val afterPath = patch.afterName

        val fileStatus = when {
            patch.isNewFile -> {
                FileStatus.ADDED
            }

            patch.isDeletedFile -> {
                FileStatus.DELETED
            }

            else -> {
                FileStatus.MODIFIED
            }
        }

        val before = getAbsolutePath(baseDir, beforePath)
        val beforeFilePath = VcsUtil.getFilePath(before, false)
        val after = getAbsolutePath(baseDir, afterPath)
        val afterFilePath = VcsUtil.getFilePath(after, false)

        var beforeRevision: ContentRevision? = null
        if (fileStatus !== FileStatus.ADDED) {
            beforeRevision = object : CurrentContentRevision(beforeFilePath) {
                override fun getRevisionNumber(): VcsRevisionNumber {
                    return TextRevisionNumber(VcsBundle.message("local.version.title"))
                }
            }
        }

        var afterRevision: ContentRevision? = null
        if (fileStatus !== FileStatus.DELETED) {
            afterRevision = object : CurrentContentRevision(beforeFilePath) {
                override fun getRevisionNumber(): VcsRevisionNumber =
                    TextRevisionNumber(VcsBundle.message("local.version.title"))

                override fun getVirtualFile(): VirtualFile? = afterFilePath.virtualFile
                override fun getFile(): FilePath = afterFilePath
                override fun getContent(): @NonNls String? {
                    when {
                        patch.isNewFile -> {
                            return patch.singleHunkPatchText
                        }

                        patch.isDeletedFile -> {
                            return null
                        }

                        else -> {
                            val localContent: String = loadLocalContent(beforeFilePath) ?: ""
                            val appliedPatch = GenericPatchApplier.apply(localContent, patch.hunks)
                            if (appliedPatch != null) {
                                return appliedPatch.patchedText
                            }

                            return patch.singleHunkPatchText
                        }
                    }
                }
            }
        }

        return Change(beforeRevision, afterRevision, fileStatus)
    }

    @Throws(VcsException::class)
    private fun loadLocalContent(beforeFilePath: FilePath): String? {
        return compute<String?, VcsException?>(ThrowableComputable {
            val file: VirtualFile? = beforeFilePath.virtualFile
            if (file == null) return@ThrowableComputable null
            val doc = FileDocumentManager.getInstance().getDocument(file)
            if (doc == null) return@ThrowableComputable null
            doc.text
        })
    }
}