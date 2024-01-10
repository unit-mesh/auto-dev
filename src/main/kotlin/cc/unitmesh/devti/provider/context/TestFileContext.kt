package cc.unitmesh.devti.provider.context

import cc.unitmesh.devti.context.ClassContext
import com.intellij.lang.Language
import com.intellij.openapi.vfs.VirtualFile

data class TestFileContext(
    val isNewFile: Boolean,
    val file: VirtualFile,
    val relatedClasses: List<ClassContext> = emptyList(),
    val testClassName: String?,
    val language: Language,
    val currentClass: ClassContext? = null,
    val imports: List<String> = emptyList(),
)