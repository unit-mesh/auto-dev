package cc.unitmesh.rust.provider

import cc.unitmesh.devti.context.ClassContext
import cc.unitmesh.devti.provider.WriteTestService
import cc.unitmesh.devti.provider.context.TestFileContext
import cc.unitmesh.rust.context.RustMethodContextBuilder
import com.intellij.execution.configurations.RunProfile
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import org.rust.cargo.runconfig.command.CargoCommandConfiguration
import org.rust.lang.RsLanguage
import org.rust.lang.core.psi.RsFunction
import org.rust.lang.core.psi.RsUseItem

class RustTestContextProvider : WriteTestService() {
    override fun runConfigurationClass(project: Project): Class<out RunProfile> = CargoCommandConfiguration::class.java

    override fun isApplicable(element: PsiElement): Boolean = element.language is RsLanguage

    override fun findOrCreateTestFile(sourceFile: PsiFile, project: Project, element: PsiElement): TestFileContext? {
        val currentObject = when (element) {
            is RsFunction -> {
                RustMethodContextBuilder().getMethodContext(element, true, false)?.format()
            }

            else -> null
        } ?: return null

        val imports = PsiTreeUtil.getChildrenOfTypeAsList(sourceFile, RsUseItem::class.java).map {
            it.text
        }

        return TestFileContext(
            false,
            sourceFile.virtualFile,
            listOf(),
            "",
            RsLanguage,
            currentObject,
            imports
        )
    }

    override fun lookupRelevantClass(project: Project, element: PsiElement): List<ClassContext> {
        return listOf()
    }

}
