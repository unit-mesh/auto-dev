package cc.unitmesh.devti.gui.block

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightVirtualFile
import kotlin.jvm.internal.Intrinsics

val AUTODEV_SNIPPET_NAME = "AutoDevSnippet"


object AutoDevSnippetFile {
    fun isAIAssistantFile(file: VirtualFile): Boolean {
        if (file is LightVirtualFile) {
            return file.getName() == AUTODEV_SNIPPET_NAME
        }

        return false
    }
}