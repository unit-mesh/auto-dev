package cc.unitmesh.devti.provider.context

import cc.unitmesh.devti.context.ClassContext
import com.intellij.lang.Language
import com.intellij.openapi.vfs.VirtualFile

data class TestFileContext(
    val isNewFile: Boolean,
    val outputFile: VirtualFile,
    val relatedClasses: List<ClassContext> = emptyList(),
    val testClassName: String?,
    val language: Language,
    /**
     * In Java, it is the current class.
     * In Kotlin, it is the current class or current function.
     * In JavaScript, it is the current class or current function.
     */
    val currentObject: String? = null,
    val imports: List<String> = emptyList(),
)