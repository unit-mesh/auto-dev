package cc.unitmesh.ide.javascript.util

import cc.unitmesh.ide.javascript.flow.DsComponent
import com.intellij.lang.ecmascript6.psi.ES6ExportDeclaration
import com.intellij.lang.ecmascript6.psi.ES6ExportDefaultAssignment
import com.intellij.lang.javascript.psi.JSFile
import com.intellij.lang.javascript.psi.JSFunctionExpression
import com.intellij.lang.javascript.psi.JSReferenceExpression
import com.intellij.lang.javascript.psi.JSVariable
import com.intellij.lang.javascript.psi.ecma6.*
import com.intellij.lang.javascript.psi.resolve.JSResolveResult
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.util.PsiTreeUtil

object ReactPsiUtil {
    private fun getExportElements(file: JSFile): List<PsiNameIdentifierOwner> {
        val exportDeclarations =
            PsiTreeUtil.getChildrenOfTypeAsList(file, ES6ExportDeclaration::class.java)

        val map = exportDeclarations.map { exportDeclaration ->
            exportDeclaration.exportSpecifiers
                .asSequence()
                .mapNotNull {
                    it.alias?.findAliasedElement()
                }
                .filterIsInstance<PsiNameIdentifierOwner>()
                .toList()
        }.flatten()

        val defaultAssignments = PsiTreeUtil.getChildrenOfTypeAsList(file, ES6ExportDefaultAssignment::class.java)
        val defaultAssignment = defaultAssignments.mapNotNull {
            val jsReferenceExpression = it.expression as? JSReferenceExpression ?: return@mapNotNull null
            val resolveReference = JSResolveResult.resolveReference(jsReferenceExpression)
            resolveReference.firstOrNull() as? PsiNameIdentifierOwner
        }

        return map + defaultAssignment
    }

    fun tsxComponentToComponent(jsFile: JSFile): List<DsComponent> = getExportElements(jsFile).map { psiElement ->
        val name = psiElement.name ?: return@map null
        val path = jsFile.virtualFile.canonicalPath ?: return@map null
        return@map when (psiElement) {
            is TypeScriptFunction -> {
                DsComponent(name = name, path)
            }

            is TypeScriptClass -> {
                DsComponent(name = name, path)
            }

            is TypeScriptVariable -> {
                val funcExpr = PsiTreeUtil.findChildrenOfType(psiElement, TypeScriptFunctionExpression::class.java)
                    .firstOrNull() ?: return@map null

                val map = funcExpr.parameterList?.parameters?.mapNotNull { parameter ->
                    if (parameter.typeElement != null) {
                        parameter.typeElement
                    } else {
                        null
                    }
                } ?: emptyList()

                DsComponent(name = name, path)
            }

            is JSVariable -> {
                val funcExpr = PsiTreeUtil.findChildrenOfType(psiElement, JSFunctionExpression::class.java)
                    .firstOrNull() ?: return@map null

                val map = funcExpr.parameterList?.parameters?.mapNotNull { parameter ->
                    val typeElement = parameter.typeElement ?: return@mapNotNull null
                    when (typeElement) {
                        is TypeScriptSingleType -> {
                            typeElement.referenceExpression?.let {
                                val resolveReference = JSResolveResult.resolveReference(it).firstOrNull()
                                println("resolveReference: ${resolveReference?.text}")
                                resolveReference
                            }
                        }

                        else -> {
                            println("unknown type: ${typeElement::class.java}")
                            null
                        }
                    }
                } ?: emptyList()

                DsComponent(name = name, path)
            }

            else -> {
                println("unknown type: ${psiElement::class.java}")
                null
            }
        }
    }.filterNotNull()
}