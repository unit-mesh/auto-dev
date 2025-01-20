// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cc.unitmesh.devti.util

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.vcs.changes.VcsIgnoreManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.ProjectScope

// https://github.com/JetBrains/intellij-community/blob/master/platform/projectModel-impl/src/com/intellij/openapi/roots/impl/ProjectFileIndexImpl.java#L32
fun isInProject(virtualFile: VirtualFile, project: Project): Boolean {
    if (ProjectFileIndex.getInstance(project).isInContent(virtualFile)) {
        return true
    }
    return false
}

fun Project.isInProject(virtualFile: VirtualFile): Boolean {
    return ProjectFileIndex.getInstance(this).isInContent(virtualFile)
}

fun Project.findFile(filename: String): VirtualFile? {
    ApplicationManager.getApplication().assertReadAccessAllowed()
    return FilenameIndex.getVirtualFilesByName(filename, ProjectScope.getProjectScope(this)).firstOrNull()
}

fun Project.findFileByPath(path: String): VirtualFile? {
    val projectPath = this.guessProjectDir()?.toNioPath()
    val realpath = projectPath?.resolve(path)
    return VirtualFileManager.getInstance().findFileByUrl("file://${realpath?.toAbsolutePath()}")
}

fun VirtualFile.canBeAdded(project: Project): Boolean {
    if (!this.isValid || this.isDirectory) return false
    if (this.fileType.isBinary || FileUtilRt.isTooLarge(this.length)) return false
    if (FileTypeManager.getInstance().isFileIgnored(this)) return false
    if (isIgnoredByVcs(project, this)) return false

    return true
}

fun VirtualFile.relativePath(project: Project): String {
    val projectDir = project.guessProjectDir()!!.toNioPath().toFile()
    val relativePath = FileUtil.getRelativePath(projectDir, this.toNioPath().toFile())
    return relativePath ?: this.path
}

fun isIgnoredByVcs(project: Project?, file: VirtualFile?): Boolean {
    val ignoreManager = VcsIgnoreManager.getInstance(project!!)
    return ignoreManager.isPotentiallyIgnoredFile(file!!)
}