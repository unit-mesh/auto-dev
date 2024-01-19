package cc.unitmesh.rust.provider

import cc.unitmesh.devti.context.ClassContext
import cc.unitmesh.devti.provider.WriteTestService
import cc.unitmesh.devti.provider.context.TestFileContext
import cc.unitmesh.rust.context.RustClassContextBuilder
import cc.unitmesh.rust.context.RustMethodContextBuilder
import com.intellij.execution.configurations.RunProfile
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import org.rust.cargo.runconfig.command.CargoCommandConfiguration
import org.rust.lang.RsLanguage
import org.rust.lang.core.psi.*

class RustTestService : WriteTestService() {
    override fun runConfigurationClass(project: Project): Class<out RunProfile> = CargoCommandConfiguration::class.java

    override fun isApplicable(element: PsiElement): Boolean {
        return element.language is RsLanguage
    }

    override fun findOrCreateTestFile(sourceFile: PsiFile, project: Project, psiElement: PsiElement): TestFileContext? {
        val testable = psiElement is RsImplItem || psiElement is RsFunction
        val element = if (!testable) {
            when (val parent = psiElement.parent) {
                is RsFunction -> parent
                is RsImplItem -> parent
                is RsStructItem -> parent
                is RsEnumItem -> parent
                else -> {
                    PsiTreeUtil.getParentOfType(psiElement, RsFunction::class.java, RsImplItem::class.java)
                }
            }
        } else {
            psiElement
        } ?: psiElement

        val currentObject = when (element) {
            is RsFunction -> {
                runReadAction {
                    RustMethodContextBuilder().getMethodContext(element, true, false)?.format()
                }
            }

            is RsImplItem -> {
                runReadAction {
                    val type = element.typeReference?.reference?.resolve() ?: return@runReadAction null
                    val classContext =
                        RustClassContextBuilder().getClassContext(type, false) ?: return@runReadAction null
                    classContext.format()
                }
            }

            else -> null
        }

        val imports = PsiTreeUtil.getChildrenOfTypeAsList(sourceFile, RsUseItem::class.java).map {
            it.text
        }

        val relevantClasses = lookupRelevantClass(project, element)

        return TestFileContext(
            false,
            sourceFile.virtualFile,
            relevantClasses,
            "",
            RsLanguage,
            currentObject,
            imports
        )
    }

    override fun lookupRelevantClass(project: Project, element: PsiElement): List<ClassContext> {
        when (element) {
            is RsFunction -> {
                val returnType = element.retType?.typeReference
                val input = element.valueParameterList?.valueParameterList?.map {
                    it.typeReference
                } ?: emptyList()

                val refs = (listOf(returnType) + input).filterNotNull()
                val types = resolveReferenceTypes(project, refs)

                return types.mapNotNull {
                    RustClassContextBuilder().getClassContext(it, false)
                }
            }
        }

        return listOf()
    }

    private fun resolveReferenceTypes(project: Project, rsTypeReferences: List<RsTypeReference>): List<PsiElement> {
        val mapNotNull = rsTypeReferences.mapNotNull {
            it.reference?.resolve()
        }

        return mapNotNull
    }
}
