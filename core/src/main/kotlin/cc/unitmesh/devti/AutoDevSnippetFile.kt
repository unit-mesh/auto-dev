package cc.unitmesh.devti

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightVirtualFile

object AutoDevSnippetFile {
    const val AUTODEV_SNIPPET_NAME = "autodev-snippet-"

    fun isSnippet(file: VirtualFile): Boolean {
        if (file !is LightVirtualFile) return false
        return file.name.startsWith("autodev-snippet-") || file.name.startsWith("DevIn-") || file.name.startsWith("devIn-")
    }

    fun naming(extension: String): String {
        val time = System.currentTimeMillis()
        return "$AUTODEV_SNIPPET_NAME$time.$extension"
    }
}