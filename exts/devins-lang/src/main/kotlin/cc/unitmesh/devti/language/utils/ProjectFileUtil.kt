package cc.unitmesh.devti.language.utils

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.ProjectScope

fun Project.lookupFile(path: String): VirtualFile? {
    val projectPath = this.guessProjectDir()?.toNioPath()
    val realpath = projectPath?.resolve(path)
    return VirtualFileManager.getInstance().findFileByUrl("file://${realpath?.toAbsolutePath()}")
}

fun Project.findFile(path: String): VirtualFile? {
    ApplicationManager.getApplication().assertReadAccessAllowed()
    val searchScope = ProjectScope.getProjectScope(this)
    val fileType: FileType = FileTypeManager.getInstance().getFileTypeByFileName(path)
    val allTypeFiles = FileTypeIndex.getFiles(fileType, searchScope)

    for (file in allTypeFiles) {
        if (file.name == path || file.path.endsWith(path)) {
            return file
        }
    }

    return null
}

fun VirtualFile.canBeAdded(): Boolean {
    if (!this.isValid || this.isDirectory) return false
    if (this.fileType.isBinary || FileUtilRt.isTooLarge(this.length)) return false

    return true
}