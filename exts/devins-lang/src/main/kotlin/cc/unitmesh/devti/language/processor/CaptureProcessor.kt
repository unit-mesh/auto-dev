package cc.unitmesh.devti.language.processor

import cc.unitmesh.devti.language.ast.action.PatternActionFuncDef
import cc.unitmesh.devti.language.ast.action.PatternProcessor
import cc.unitmesh.devti.language.provider.PsiCapture
import cc.unitmesh.devti.language.utils.lookupFile
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager

object CaptureProcessor: PatternProcessor {
    override val type: PatternActionFuncDef = PatternActionFuncDef.CAPTURE

    fun execute(myProject: Project, fileName: String, nodeType: String): Any {
        // first lookup file in the file system
        val lookupFile = myProject.lookupFile(fileName) ?: return "File not found: $fileName"

        // convert to psi
        val psiFile = ReadAction.compute<PsiFile?, Throwable> {
            PsiManager.getInstance(myProject).findFile(lookupFile)
        } ?: return "Failed to find PSI file for $fileName"

        val language = psiFile.language

        PsiCapture.provide(language)?.let {
            val text = ReadAction.compute<String, Throwable> {
                psiFile.text
            }

            return it.capture(text, nodeType)
        }

        // execute the capture function
        val result = psiFile.children.filter {
            it.node.elementType.toString() == nodeType
        }

        return result
    }
}
