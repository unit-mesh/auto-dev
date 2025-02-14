package cc.unitmesh.idea.provider

import cc.unitmesh.devti.provider.RelatedClassesProvider
import cc.unitmesh.idea.context.JavaContextCollection
import cc.unitmesh.idea.service.JavaTypeUtil.resolveByType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.psi.*
import com.intellij.psi.util.*
import com.intellij.testIntegration.TestFinderHelper

class JavaRelatedClassesProvider : RelatedClassesProvider {
    override fun lookup(element: PsiElement): List<PsiClass> {
        return when (element) {
            is PsiMethod -> findRelatedClasses(element)
                .flatMap { findSuperClasses(it) }
                .map { cleanUp(it) }
                .toList()
                .distinct()

            is PsiClass -> findRelatedClasses(element)
            else -> emptyList()
        }
    }

    override fun lookup(element: PsiFile): List<PsiElement> {
        return when (element) {
            is PsiJavaFile -> findRelatedClasses(element.classes.first()) + lookupTestFile(element.classes.first())
            else -> emptyList()
        }
    }

    private fun lookupTestFile(psiElement: PsiElement): List<PsiElement> {
        if (!psiElement.isValid) return emptyList()

        return ApplicationManager.getApplication().executeOnPooledThread<List<PsiElement>> {
            runReadAction {
                val isTest = TestFinderHelper.isTest(psiElement)
                if (isTest) return@runReadAction emptyList()

                TestFinderHelper.findTestsForClass(psiElement)
            }?.toList() ?: emptyList()
        }.get() ?: emptyList()
    }

    private fun findRelatedClasses(clazz: PsiClass): List<PsiClass> {
        if (!clazz.isValid) return emptyList()

        val qualifiedName = clazz.qualifiedName
        return ApplicationManager.getApplication().executeOnPooledThread<List<PsiClass>?> {
            runReadAction {
                val methods = clazz.allMethods.flatMap { findRelatedClasses(it) }
                val fieldsTypes: List<PsiClass> = clazz.fields.mapNotNull {
                    when (it.type) {
                        is PsiClassType -> {
                            val resolve = (it.type as PsiClassType).resolve() ?: return@mapNotNull null
                            if (resolve.qualifiedName == qualifiedName) return@mapNotNull null

                            if (isJavaBuiltin(resolve.qualifiedName) == true || JavaContextCollection.isPopularFramework(resolve.qualifiedName) == true) {
                                return@mapNotNull null
                            }

                            resolve
                        }

                        else -> null
                    }
                }
                return@runReadAction (fieldsTypes + methods).distinct()
            }
        }?.get() ?: emptyList()
    }

    /**
     * Finds related classes to the given PsiMethod by analyzing its parameters, return type, and generic types.
     *
     * @param method the PsiMethod for which related classes need to be found
     * @return a list of PsiClass instances that are related to the given PsiMethod, filtered to include only classes that are part of the project content
     */
    private fun findRelatedClasses(method: PsiMethod): List<PsiClass> = runReadAction {
        if (!method.isValid) return@runReadAction emptyList()

        val parameters = method.parameterList.parameters
        val parameterTypes = parameters.map { it.type }

        val genericTypes = parameters.flatMap { (it.type as? PsiClassType)?.parameters?.toList() ?: emptyList() }

        val returnType = if (method.returnTypeElement?.type != null) {
            resolveByType(method.returnTypeElement?.type!!).values
        } else {
            emptyList()
        }

        val mentionedTypes = parameterTypes + genericTypes

        val filterIsInstance = mentionedTypes.filterIsInstance<PsiClassType>()
            .distinct()

        return@runReadAction ApplicationManager.getApplication().executeOnPooledThread<List<PsiClass>> {
            return@executeOnPooledThread returnType + filterIsInstance
                .mapNotNull { runReadAction { it.resolve() } }
                .filter { isProjectContent(it) }
                .toList()
        }.get()
    }

    /**
     * Cleans up a given PsiClass by removing unnecessary elements such as method bodies, method comments, and any other removable members.
     *
     * @param psiClass the PsiClass to be cleaned up
     * @return a new PsiClass with the unnecessary elements removed
     */
    private fun cleanUp(psiClass: PsiClass): PsiClass {
        val psiElement = psiClass.copy() as PsiClass
        psiElement.containingFile.setName(psiClass.containingFile.name)

        val members = PsiTreeUtil.findChildrenOfType(psiElement, PsiMember::class.java)

        members.filterIsInstance<PsiMethod>().forEach {
            it.body?.delete()
            it.docComment?.delete()
        }
        members.filter { canBeRemoved(it) }.forEach { it.delete() }

        psiElement.docComment?.delete()
        return psiElement
    }

    private fun findSuperClasses(psiClass: PsiClass): List<PsiClass> {
        val superClass = psiClass.superClass ?: return emptyList()
        if (isProjectContent(superClass)) {
            return listOf(psiClass.superClass!!, psiClass)
        }

        if (isProjectContent(psiClass)) {
            return listOf(psiClass)
        }

        return emptyList()
    }

    private fun canBeRemoved(member: PsiMember): Boolean {
        if (member.modifierList?.hasModifierProperty("public") == true) return false
        return member.annotations.isEmpty()
    }

    private fun isProjectContent(element: PsiElement): Boolean {
        val virtualFile = PsiUtil.getVirtualFile(element) ?: return false
        return ApplicationManager.getApplication().executeOnPooledThread<Boolean> {
            runReadAction {
                ProjectFileIndex.getInstance(element.project).isInSourceContent(virtualFile)
            }
        }.get()
    }

    fun isJavaBuiltin(qualifiedName: String?) = qualifiedName?.startsWith("java.")
}