// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cc.unitmesh.devti.util

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.vcs.changes.VcsIgnoreManager
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiManager
import com.intellij.util.concurrency.annotations.RequiresReadLock
import org.jetbrains.annotations.Contract
import org.jetbrains.annotations.SystemIndependent
import java.io.IOException
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.pathString

// https://github.com/JetBrains/intellij-community/blob/master/platform/projectModel-impl/src/com/intellij/openapi/roots/impl/ProjectFileIndexImpl.java#L32
fun isInProject(virtualFile: VirtualFile, project: Project): Boolean {
    return runReadAction { PsiManager.getInstance(project).findFile(virtualFile) } != null
}

fun Project.isInProject(virtualFile: VirtualFile): Boolean {
    return ProjectFileIndex.getInstance(this).isInContent(virtualFile)
}

fun Project.findFile(filename: String): VirtualFile? {
    return this.guessProjectDir()!!.findFileByRelativePath(filename)
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

fun virtualFile(editor: Editor?): VirtualFile? {
    if (editor == null) return null
    return FileDocumentManager.getInstance().getFile(editor.document)
}

fun VirtualFile.validOrNull() = if (isValid) this else null

val VirtualFile.isFile: Boolean
    get() = isValid && !isDirectory

fun VirtualFile.readText(): String {
    return VfsUtilCore.loadText(this)
}

@RequiresReadLock
fun VirtualFile.findFileOrDirectory(relativePath: @SystemIndependent String): VirtualFile? {
    return getResolvedVirtualFile(relativePath) { name, _ ->
        findChild(name) ?: return null // return from findFileOrDirectory
    }
}

@RequiresReadLock
fun VirtualFile.findFile(relativePath: @SystemIndependent String): VirtualFile? {
    val file = findFileOrDirectory(relativePath) ?: return null
    if (!file.isFile) {
        throw IOException("""
      |Expected file instead of directory: $file
      |  basePath = $path
      |  relativePath = $relativePath
    """.trimMargin())
    }
    return file
}

private inline fun VirtualFile.getResolvedVirtualFile(
    relativePath: String,
    getChild: VirtualFile.(String, Boolean) -> VirtualFile
): VirtualFile {
    val (baseVirtualFile, normalizedRelativePath) = relativizeToClosestAncestor(relativePath)
    var virtualFile = baseVirtualFile
    if (normalizedRelativePath.pathString.isNotEmpty()) {
        val names = normalizedRelativePath.map { it.pathString }
        for ((i, name) in names.withIndex()) {
            if (!virtualFile.isDirectory) {
                throw IOException("""
          |Expected directory instead of file: $virtualFile
          |  basePath = $path
          |  relativePath = $relativePath
        """.trimMargin())
            }
            virtualFile = virtualFile.getChild(name, i == names.lastIndex)
        }
    }
    return virtualFile
}

fun Path.relativizeToClosestAncestor(relativePath: String): Pair<Path, Path> {
    val normalizedPath = getResolvedPath(relativePath)
    val normalizedBasePath = checkNotNull(findAncestor(this, normalizedPath)) {
        """
      |Cannot resolve normalized base path for: $normalizedPath
      |  basePath = $this
      |  relativePath = $relativePath
    """.trimMargin()
    }
    val normalizedRelativePath = normalizedBasePath.relativize(normalizedPath)
    return normalizedBasePath to normalizedRelativePath
}

@Contract(pure = true)
fun findAncestor(path1: Path, path2: Path): Path? {
    var ancestor: Path? = path1
    while (ancestor != null && !path2.startsWith(ancestor)) {
        ancestor = ancestor.getParent()
    }
    return ancestor
}

private fun VirtualFile.relativizeToClosestAncestor(
    relativePath: String
): Pair<VirtualFile, Path> {
    val basePath = Paths.get(path)
    val (normalizedBasePath, normalizedRelativePath) = basePath.relativizeToClosestAncestor(relativePath)
    var baseVirtualFile = this
    repeat(basePath.nameCount - normalizedBasePath.nameCount) {
        baseVirtualFile = checkNotNull(baseVirtualFile.parent) {
            """
        |Cannot resolve base virtual file for $baseVirtualFile
        |  basePath = $path
        |  relativePath = $relativePath
      """.trimMargin()
        }
    }
    return baseVirtualFile to normalizedRelativePath
}

fun Path.getResolvedPath(relativePath: String): Path {
    return resolve(relativePath).normalize()
}
