package cc.unitmesh.rust.provider

import cc.unitmesh.devti.context.builder.CodeModifier
import com.intellij.lang.Language
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import org.rust.lang.RsLanguage
import org.rust.lang.core.psi.RsFile
import org.rust.lang.core.psi.RsModItem

class RustCodeModifier : CodeModifier {
    override fun isApplicable(language: Language): Boolean = language is RsLanguage

    override fun insertTestCode(sourceFile: VirtualFile, project: Project, code: String): Boolean {
        // in Rust, lang the test code is inserted into the source file
        // 1. check mod test exits
        val psiFile = PsiManager.getInstance(project).findFile(sourceFile) ?: return false
        val rsFile = psiFile as? RsFile ?: return false
        val testMod = runReadAction {
            PsiTreeUtil.findChildOfType(rsFile, RsModItem::class.java)
        }
        if (testMod == null) {
            insertClass(sourceFile, project, code)
        } else {
            insertMethod(sourceFile, project, code)
        }

        return true
    }

    override fun insertMethod(sourceFile: VirtualFile, project: Project, code: String): Boolean {
        val psiFile = PsiManager.getInstance(project).findFile(sourceFile) ?: return false
        val lastElement = psiFile.lastChild ?: return false

        return WriteCommandAction.runWriteCommandAction<Boolean>(project) {
            val document = psiFile.viewProvider.document!!
            document.insertString(lastElement.textOffset, code)

            true
        }
    }

    override fun insertClass(sourceFile: VirtualFile, project: Project, code: String): Boolean {
        return WriteCommandAction.runWriteCommandAction<Boolean>(project) {
            val psiFile = PsiManager.getInstance(project).findFile(sourceFile) ?: return@runWriteCommandAction false
            val document = psiFile.viewProvider.document!!
            document.insertString(document.textLength, code)

            true
        }
    }
}
