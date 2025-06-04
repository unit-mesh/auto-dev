package cc.unitmesh.devti.language.compiler.processor

import cc.unitmesh.devti.language.psi.DevInFile
import cc.unitmesh.devti.language.psi.DevInTypes
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.guessProjectDir
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType

/**
 * Processor for comments, including flow control comments
 */
class CommentsProcessor : DevInElementProcessor {
    
    companion object {
        private const val FLOW_FLAG = "[flow]:"
    }
    
    override suspend fun process(element: PsiElement, context: CompilerContext): ProcessResult {
        val text = runReadAction { element.text }
        
        if (text.startsWith(FLOW_FLAG)) {
            val fileName = text.substringAfter(FLOW_FLAG).trim()
            val content = context.project.guessProjectDir()?.findFileByRelativePath(fileName)?.let { virtualFile ->
                virtualFile.inputStream.bufferedReader().use { reader -> reader.readText() }
            }
            
            if (content != null) {
                val devInFile = DevInFile.fromString(context.project, content)
                context.result.nextJob = devInFile
            }
        }
        
        return ProcessResult(success = true)
    }
    
    override fun canProcess(element: PsiElement): Boolean {
        return element.elementType == DevInTypes.COMMENTS
    }
}
