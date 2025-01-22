package cc.unitmesh.devti.language.utils

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.ProjectScope

fun Project.lookupFile(path: String): VirtualFile? {
    val projectPath = this.guessProjectDir()?.toNioPath()
    val realpath = projectPath?.resolve(path)
    return VirtualFileManager.getInstance().findFileByUrl("file://${realpath?.toAbsolutePath()}")
}

fun Project.findFile(filename: String, caseSensitively: Boolean = true): VirtualFile? {
    ApplicationManager.getApplication().assertReadAccessAllowed()
    val currentTask = ApplicationManager.getApplication().executeOnPooledThread<VirtualFile?> {
        val searchedFiles = runReadAction {
                FilenameIndex.getVirtualFilesByName(filename, caseSensitively, ProjectScope.getProjectScope(this))
            }
        return@executeOnPooledThread searchedFiles.firstOrNull()
    }

    return currentTask.get()
}

// getVirtualFilesByNamesIgnoringCase

fun VirtualFile.canBeAdded(): Boolean {
    if (!this.isValid || this.isDirectory) return false
    if (this.fileType.isBinary || FileUtilRt.isTooLarge(this.length)) return false

    return true
}