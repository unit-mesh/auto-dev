package cc.unitmesh.devti.language

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.vfs.VirtualFile
import javax.swing.Icon

class DevInFileType : LanguageFileType(DevInLanguage) {
    override fun getName(): String = "DevInFile"

    override fun getIcon(): Icon = DevInIcons.DEFAULT

    override fun getDefaultExtension(): String = "devin"

    override fun getCharset(file: VirtualFile, content: ByteArray): String = "UTF-8"

    override fun getDescription(): String = "DevIn file"

    companion object {
        val INSTANCE: FileType = DevInFileType()
    }

}