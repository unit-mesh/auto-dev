package cc.unitmesh.devti.language.compiler.processor

import cc.unitmesh.devti.language.compiler.DevInsCompiler
import cc.unitmesh.devti.language.psi.*
import com.intellij.openapi.application.runReadAction
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import kotlinx.coroutines.runBlocking

/**
 * Processor for Velocity expressions
 */
class VelocityExprProcessor : DevInElementProcessor {
    
    override suspend fun process(element: PsiElement, context: CompilerContext): ProcessResult {
        val velocityExpr = element as DevInVelocityExpr
        context.logger.info("Velocity expression found: ${runReadAction { element.text }}")
        
        handleNextSiblingForChild(velocityExpr, context) { next ->
            if (next is DevInIfExpr) {
                handleNextSiblingForChild(next, context) {
                    when (it) {
                        is DevInIfClause, is DevInElseifClause, is DevInElseClause -> {
                            handleNextSiblingForChild(it, context) {
                                runBlocking { processIfClause(it, context) }
                            }
                        }
                        else -> context.appendOutput(it.text)
                    }
                }
            } else {
                context.appendOutput(next.text)
            }
        }
        
        return ProcessResult(success = true)
    }
    
    private fun handleNextSiblingForChild(
        element: PsiElement?, 
        context: CompilerContext,
        handle: (PsiElement) -> Unit
    ) {
        var child: PsiElement? = element?.firstChild
        while (child != null && !context.isError()) {
            handle(child)
            child = child.nextSibling
        }
    }
    
    private suspend fun processIfClause(clauseContent: PsiElement, context: CompilerContext) {
        when (clauseContent) {
            is DevInExpr -> {
                addVariable(clauseContent, context)
                if (!context.isError()) {
                    context.appendOutput(clauseContent.text)
                }
            }
            
            is DevInVelocityBlock -> {
                DevInFile.fromString(context.project, clauseContent.text).let { file ->
                    val compile = DevInsCompiler(context.project, file).compile()
                    compile.let {
                        context.appendOutput(it.output)
                        context.variableTable.addVariable(it.variableTable)
                        context.setError(it.hasError)
                    }
                }
            }
            
            else -> {
                context.appendOutput(clauseContent.text)
            }
        }
    }
    
    private fun addVariable(psiElement: PsiElement?, context: CompilerContext) {
        if (psiElement == null) return
        
        val queue = java.util.LinkedList<PsiElement>()
        queue.push(psiElement)
        
        val variableProcessor = VariableProcessor()
        
        while (!queue.isEmpty() && !context.isError()) {
            val e = queue.pop()
            if (e.firstChild?.elementType == DevInTypes.VARIABLE_START) {
                variableProcessor.processVariable(e.firstChild, context)
            } else {
                e.children.forEach {
                    queue.push(it)
                }
            }
        }
    }
    
    override fun canProcess(element: PsiElement): Boolean {
        return element.elementType == DevInTypes.VELOCITY_EXPR
    }
}
