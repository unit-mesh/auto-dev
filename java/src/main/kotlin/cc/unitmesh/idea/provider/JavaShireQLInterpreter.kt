package cc.unitmesh.idea.provider

import cc.unitmesh.devti.devins.shireql.JvmShireQLFuncType
import cc.unitmesh.devti.language.ast.shireql.ShireQLInterpreter
import com.intellij.lang.Language
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import com.intellij.psi.search.searches.ClassInheritorsSearch


class JavaShireQLInterpreter : ShireQLInterpreter {
    override fun supportsMethod(language: Language, methodName: String): List<String> {
        if (language.id != "JAVA") return emptyList()

        return JvmShireQLFuncType.entries.map { it.methodName }
    }

    /**
     * clazz.getName() or clazz.extensions
     */
    override fun resolveCall(element: PsiElement, methodName: String, arguments: List<Any>): Any {
        // is of method
        if (methodName.endsWith("Of")) {
            return this.resolveOfTypedCall(element.project, methodName, arguments)
        }

        return when (element) {
            is PsiClass -> {

                when (methodName) {
                    JvmShireQLFuncType.GET_NAME.methodName -> element.name!!
                    JvmShireQLFuncType.NAME.methodName -> element.name!!
                    JvmShireQLFuncType.EXTENDS.methodName -> element
                        .extendsList?.referencedTypes?.map { it.resolve() }
                        ?: emptyList<PsiClass>()

                    JvmShireQLFuncType.IMPLEMENTS.methodName -> element
                        .implementsList?.referencedTypes?.map { it.resolve() }
                        ?: emptyList<PsiClass>()

                    JvmShireQLFuncType.METHOD_CODE_BY_NAME.methodName -> element
                        .methods
                        .filter { it.name == arguments.first() }

                    JvmShireQLFuncType.FIELD_CODE_BY_NAME.methodName -> element
                        .fields
                        .filter { it.name == arguments.first() }

                    else -> ""
                }
            }

            else -> ""
        }
    }

    override fun resolveOfTypedCall(project: Project, methodName: String, arguments: List<Any>): Any {
        // get first argument for infer type
        val firstArgument = arguments.firstOrNull().toString()
        if (firstArgument.isBlank()) {
            logger<JavaShireQLInterpreter>().warn("Cannot find first argument")
            return ""
        }

        return when (methodName) {
            JvmShireQLFuncType.SUBCLASSES_OF.methodName -> {
                val facade = JavaPsiFacade.getInstance(project)

                val psiClass = facade.findClass(firstArgument, GlobalSearchScope.projectScope(project))
                if (psiClass == null) {
                    logger<JavaShireQLInterpreter>().warn("Cannot find class: $firstArgument")
                    return ""
                }

                val map: List<PsiClass> =
                    ClassInheritorsSearch.search(psiClass, GlobalSearchScope.projectScope(project), true).map { it }
                map
            }

            JvmShireQLFuncType.ANNOTATED_OF.methodName -> {
                val facade = JavaPsiFacade.getInstance(project)
                val annotationClass =
                    facade.findClass(firstArgument, GlobalSearchScope.allScope(project))

                if (annotationClass == null) {
                    logger<JavaShireQLInterpreter>().warn("Cannot find annotation class: $firstArgument")
                    return ""
                }

                val classes = AnnotatedElementsSearch
                    .searchPsiClasses(annotationClass, GlobalSearchScope.projectScope(project))
                    .findAll()

                classes.toList()
            }

            JvmShireQLFuncType.SUPERCLASS_OF.methodName -> {
                val psiClass = searchClass(project, firstArgument) ?: return ""
                psiClass.superClass ?: ""
            }

            JvmShireQLFuncType.IMPLEMENTS_OF.methodName -> {
                val psiClass = searchClass(project, firstArgument) ?: return emptyList<String>()
                psiClass.implementsList?.referencedTypes ?: emptyList<String>()
            }

            else -> {
                logger<JavaShireQLInterpreter>().error("Cannot find method: $methodName")
            }
        }
    }

    private fun searchClass(project: Project, className: String): PsiClass? {
        val scope = GlobalSearchScope.allScope(project)
        val psiFacade = JavaPsiFacade.getInstance(project)
        return psiFacade.findClass(className, scope)
    }
}
