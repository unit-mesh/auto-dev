package cc.unitmesh.diagram.idea

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.vfs.VirtualFile
import javax.swing.Icon

/**
 * File type for Graphviz DOT files
 * Similar to JdlFileType in JHipster UML implementation
 */
class DotFileType private constructor() : FileType {

    companion object {
        @JvmField
        val INSTANCE = DotFileType()
    }

    override fun getName(): String = "DOT"

    override fun getDescription(): String = "Graphviz DOT file"

    override fun getDefaultExtension(): String = "dot"

    override fun getIcon(): Icon? = IconLoader.getIcon("/icons/autodev-graphviz.svg", DotFileType::class.java)

    override fun getCharset(file: VirtualFile, content: ByteArray): String = "UTF-8"

    override fun isBinary(): Boolean = false

    override fun isReadOnly(): Boolean = false
}

