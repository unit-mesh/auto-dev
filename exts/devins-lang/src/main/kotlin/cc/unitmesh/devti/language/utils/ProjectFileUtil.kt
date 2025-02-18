package cc.unitmesh.devti.language.utils

import com.intellij.openapi.application.ApplicationManager
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
    return FilenameIndex.getVirtualFilesByName(filename, ProjectScope.getContentScope(this)).firstOrNull()
}

fun VirtualFile.canBeAdded(): Boolean {
    if (!this.isValid || this.isDirectory) return false
    if (this.fileType.isBinary || FileUtilRt.isTooLarge(this.length)) return false

    return true
}