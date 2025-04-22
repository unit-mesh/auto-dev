package cc.unitmesh.devti.language.ast.shireql

import cc.unitmesh.devti.language.ast.FunctionStatementProcessor
import cc.unitmesh.devti.language.ast.HobbitHole
import cc.unitmesh.devti.language.ast.MethodCall
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import java.lang.reflect.Method
import java.util.*

class ShireQLProcessor(override val myProject: Project, hole: HobbitHole) :
    FunctionStatementProcessor(myProject, hole) {

    override fun <T : Any> invokeMethodOrField(methodCall: MethodCall, element: T): Any? {
        val methodName = methodCall.methodName.display()
        val objectName = methodCall.objectName.display()

        val methodArgs = methodCall.arguments
        if (element is PsiElement) {
            ShireQLInterpreter.provide(element.language)?.let { psiQLInterpreter ->
                val hasPqlInterpreter = psiQLInterpreter.supportsMethod(element.language, methodName).any {
                    it == methodName
                }

                if (hasPqlInterpreter) {
                    return runReadAction {
                        psiQLInterpreter.resolveCall(element, methodName, methodCall.parameters() ?: emptyList())
                    }
                }
            }
        }

        val isField = methodArgs == null

        if (isField) {
            val field = element.javaClass.fields.find {
                it.name == methodName
            }

            if (field != null) {
                return field.get(element)
            }
        }

        // use reflection to call method
        val allMethods = element.javaClass.methods
        val method = allMethods.find {
            it.name == methodName
        }
        if (method != null) {
            if (methodArgs == null) {
                return method.invoke(element)
            }

            return method.invoke(element, methodArgs)
        }

        if (isField) {
            // maybe getter, we try to find getter, first upper case method name first letter
            val getterName = "get${
                methodName.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                }
            }"
            val getter = allMethods.find {
                it.name == getterName
            }

            if (getter != null) {
                return getter.invoke(element)
            }
        }

        // if not found, show error log
        return showErrorLog(allMethods, element, methodName, objectName)
    }

    private fun <T : Any> showErrorLog(
        allMethods: Array<out Method>,
        element: T,
        methodName: String,
        objectName: String,
    ): Nothing? {
        val supportMethodNames: List<String> = allMethods.map { it.name }
        val supportFieldNames: List<String> = element.javaClass.fields.map { it.name }

        logger<ShireQLProcessor>().error(
            """
            method or field not found: $objectName.$methodName
            supported methods: $supportMethodNames
            supported fields: $supportFieldNames
            """.trimIndent()
        )
        return null
    }
}
