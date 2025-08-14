package cc.unitmesh.diagram.idea

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.vfs.VirtualFile
import javax.swing.Icon

/**
 * File type for Mermaid files
 */
class MermaidFileType private constructor() : FileType {

    companion object {
        @JvmField
        val INSTANCE = MermaidFileType()
    }

    override fun getName(): String = "Mermaid"

    override fun getDescription(): String = "Mermaid diagram file"

    override fun getDefaultExtension(): String = "mmd"

    override fun getIcon(): Icon? = IconLoader.getIcon("/icons/autodev-mermaid.svg", MermaidFileType::class.java)

    override fun getCharset(file: VirtualFile, content: ByteArray): String = "UTF-8"

    override fun isBinary(): Boolean = false

    override fun isReadOnly(): Boolean = false
}