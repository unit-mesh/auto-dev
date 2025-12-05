package cc.unitmesh.devti.language.compiler.processor

import cc.unitmesh.devti.language.compiler.HobbitHoleParser
import cc.unitmesh.devti.language.psi.DevInFrontMatterHeader
import cc.unitmesh.devti.language.psi.DevInTypes
import com.intellij.openapi.application.runReadAction
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType

/**
 * Processor for front matter headers
 */
class FrontMatterProcessor : DevInElementProcessor {
    
    override suspend fun process(element: PsiElement, context: CompilerContext): ProcessResult {
        when (element.elementType) {
            DevInTypes.FRONTMATTER_START -> {
                val nextElement = runReadAction {
                    PsiTreeUtil.findChildOfType(element.parent, DevInFrontMatterHeader::class.java)
                }
                if (nextElement != null) {
                    context.result.config = runReadAction { HobbitHoleParser.parse(nextElement) }
                }
            }
            
            DevInTypes.FRONT_MATTER_HEADER -> {
                context.result.config = runReadAction { 
                    HobbitHoleParser.parse(element as DevInFrontMatterHeader) 
                }
            }
        }
        
        return ProcessResult(success = true)
    }
    
    override fun canProcess(element: PsiElement): Boolean {
        return element.elementType in setOf(
            DevInTypes.FRONTMATTER_START,
            DevInTypes.FRONT_MATTER_HEADER
        )
    }
}
