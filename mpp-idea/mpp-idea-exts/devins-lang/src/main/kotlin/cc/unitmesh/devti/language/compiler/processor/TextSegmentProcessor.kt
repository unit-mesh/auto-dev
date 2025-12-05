package cc.unitmesh.devti.language.compiler.processor

import cc.unitmesh.devti.language.psi.DevInTypes
import com.intellij.lang.parser.GeneratedParserUtilBase.DUMMY_BLOCK
import com.intellij.openapi.application.runReadAction
import com.intellij.psi.PsiElement
import com.intellij.psi.TokenType.WHITE_SPACE
import com.intellij.psi.util.elementType

/**
 * Processor for simple text elements that should be appended to output as-is
 */
class TextSegmentProcessor : DevInElementProcessor {
    
    override suspend fun process(element: PsiElement, context: CompilerContext): ProcessResult {
        val text = runReadAction { element.text }
        
        when (element.elementType) {
            DevInTypes.TEXT_SEGMENT -> context.appendOutput(text)
            DevInTypes.NEWLINE -> context.appendOutput("\n")
            DevInTypes.MARKDOWN_HEADER -> context.appendOutput("#[[${text}]]#")
            WHITE_SPACE, DUMMY_BLOCK -> context.appendOutput(text)
            else -> {
                context.appendOutput(text)
                context.logger.warn("Unknown element type in TextSegmentProcessor: ${element.elementType}")
            }
        }
        
        return ProcessResult(success = true)
    }
    
    override fun canProcess(element: PsiElement): Boolean {
        return element.elementType in setOf(
            DevInTypes.TEXT_SEGMENT,
            DevInTypes.NEWLINE,
            DevInTypes.MARKDOWN_HEADER,
            WHITE_SPACE,
            DUMMY_BLOCK
        )
    }
}
