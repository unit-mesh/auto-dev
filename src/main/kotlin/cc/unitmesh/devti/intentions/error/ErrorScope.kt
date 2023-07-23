package cc.unitmesh.devti.intentions.error

import com.intellij.openapi.vfs.VirtualFile

class ErrorScope(val lineStart: Int, val lineFinish: Int, val text: String, val thisVirtualFile: VirtualFile) {
    fun containsLineNumber(lineNumber: Int, virtualFile: VirtualFile): Boolean {
        return lineNumber in lineStart..lineFinish && thisVirtualFile == virtualFile
    }
}