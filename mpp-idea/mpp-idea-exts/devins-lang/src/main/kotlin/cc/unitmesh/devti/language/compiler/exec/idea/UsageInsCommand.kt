package cc.unitmesh.devti.language.compiler.exec.idea

import cc.unitmesh.devti.command.InsCommand
import cc.unitmesh.devti.command.dataprovider.BuiltinCommand
import cc.unitmesh.devti.language.compiler.error.DEVINS_ERROR
import cc.unitmesh.devti.provider.RelatedClassesProvider
import cc.unitmesh.devti.provider.devins.DevInsSymbolProvider
import cc.unitmesh.devti.util.relativePath
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiDocumentManager

class UsageInsCommand(val myProject: Project, private val symbol: String) : InsCommand {
    override val commandName: BuiltinCommand = BuiltinCommand.USAGE

    override suspend fun execute(): String? {
        val elements = DevInsSymbolProvider.all().map {
            runReadAction { it.resolveElement(myProject, symbol) }
        }.flatten()

        if (elements.isEmpty()) return "$DEVINS_ERROR: No symbol found for $symbol"

        val psiElements = elements.mapNotNull {
            RelatedClassesProvider.provide(it.language)?.lookupCaller(myProject, it)
        }.flatten()

        if (psiElements.isEmpty()) return "$DEVINS_ERROR: No usage found for $symbol"

        return "Here is related to $symbol usage:\n\n" + psiElements.joinToString("\n\n") { element: PsiNamedElement ->
            runReadAction { 
                val (filePath, lineRange) = getFileAndLineInfo(element)
                "Follow code from $filePath (line $lineRange):\n${element.text}"
            }
        }
    }
    
    private fun getFileAndLineInfo(element: PsiElement): Pair<String, String> {
        val containingFile = element.containingFile
        val virtualFile = containingFile.virtualFile
        val filePath = virtualFile?.relativePath(myProject) ?: "unknown file"
        
        val document = PsiDocumentManager.getInstance(myProject).getDocument(containingFile)
        val startLine = document?.getLineNumber(element.textRange.startOffset)?.plus(1) ?: 0
        val endLine = document?.getLineNumber(element.textRange.endOffset)?.plus(1) ?: 0
        
        val lineRange = if (startLine == endLine) "$startLine" else "$startLine-$endLine"

        return Pair(filePath, lineRange)
    }
}
