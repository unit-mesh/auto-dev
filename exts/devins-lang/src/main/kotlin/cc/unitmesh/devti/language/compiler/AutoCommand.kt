package cc.unitmesh.devti.language.compiler

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager

interface AutoCommand {
    fun execute(): String?
}

fun Project.lookupFile(path: String): VirtualFile? {
    val projectPath = this.guessProjectDir()?.toNioPath()
    val realpath = projectPath?.resolve(path)

    val virtualFile =
        VirtualFileManager.getInstance().findFileByUrl("file://${realpath?.toAbsolutePath()}")
    return virtualFile
}

