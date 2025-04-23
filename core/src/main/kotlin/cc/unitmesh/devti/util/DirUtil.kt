package cc.unitmesh.devti.util

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.vfs.VirtualFile

object DirUtil {
    private val pathSeparator = "/"
    fun getOrCreateDirectory(baseDir: VirtualFile, path: String): VirtualFile {
        var currentDir = baseDir
        val pathSegments = path.split(pathSeparator).filter { it.isNotEmpty() }

        for (segment in pathSegments) {
            val childDir = currentDir.findChild(segment)
            currentDir = childDir ?: runWriteAction { currentDir.createChildDirectory(DirUtil.javaClass, segment) }
        }

        return currentDir
    }

}
