package cc.unitmesh.devti.language.utils

import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.vfs.VirtualFile

fun canBeAdded(file: VirtualFile): Boolean {
    if (!file.isValid || file.isDirectory) return false
    if (file.fileType.isBinary || FileUtilRt.isTooLarge(file.length)) return false

    return true
}