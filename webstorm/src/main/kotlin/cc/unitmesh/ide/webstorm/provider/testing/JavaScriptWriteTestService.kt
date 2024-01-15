package cc.unitmesh.ide.webstorm.provider.testing

import cc.unitmesh.devti.context.ClassContext
import cc.unitmesh.devti.provider.WriteTestService
import cc.unitmesh.devti.provider.context.TestFileContext
import cc.unitmesh.ide.webstorm.LanguageApplicableUtil
import com.intellij.execution.configurations.RunProfile
import com.intellij.lang.ecmascript6.psi.ES6ExportDeclaration
import com.intellij.lang.ecmascript6.psi.ES6ExportDefaultAssignment
import com.intellij.lang.javascript.buildTools.npm.rc.NpmRunConfiguration
import com.intellij.lang.javascript.frameworks.commonjs.CommonJSUtil
import com.intellij.lang.javascript.psi.*
import com.intellij.lang.javascript.psi.ecmal4.JSAttributeList
import com.intellij.lang.javascript.psi.ecmal4.JSAttributeListOwner
import com.intellij.lang.javascript.psi.ecmal4.JSClass
import com.intellij.lang.javascript.psi.ecmal4.JSQualifiedNamedElement
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.util.PsiTreeUtil
import kotlin.io.path.Path
import kotlin.io.path.nameWithoutExtension


class JavaScriptWriteTestService : WriteTestService() {
    override fun runConfigurationClass(project: Project): Class<out RunProfile> {
        return NpmRunConfiguration::class.java
    }

    override fun isApplicable(element: PsiElement): Boolean {
        val sourceFile: PsiFile = element.containingFile ?: return false
        return LanguageApplicableUtil.isJavaScriptApplicable(sourceFile.language)
    }

    override fun findOrCreateTestFile(sourceFile: PsiFile, project: Project, element: PsiElement): TestFileContext? {
        val language = sourceFile.language
        val targetFilePath = sourceFile.name.replace(".ts", ".test.ts")

        val elementToTest = getElementToTest(element) ?: return null
        val elementName = elementName(elementToTest) ?: return null

        val testFile = LocalFileSystem.getInstance().findFileByPath(targetFilePath)
        if (testFile != null) {
            return TestFileContext(false, testFile, emptyList(), null, language, null)
        }

        val testFileName = Path(targetFilePath).nameWithoutExtension
        val testFileText = ""
        val testFilePsi = ReadAction.compute<PsiFile, Throwable> {
            PsiFileFactory.getInstance(project).createFileFromText(testFileName, language, testFileText)
        }

        return TestFileContext(true, testFilePsi.virtualFile, emptyList(), elementName, language, null)
    }

    override fun lookupRelevantClass(project: Project, element: PsiElement): List<ClassContext> {
        return emptyList()
    }

    /**
     * In JavaScript/TypeScript a testable element is a function, a class or a variable.
     *
     * Function:
     * ```javascript
     * function testableFunction() {}
     * export testableFunction
     * ```
     *
     * Class:
     * ```javascript
     * export class TestableClass {}
     * ```
     *
     * Variable:
     * ```javascript
     * var functionA = function() {}
     * export functionA
     * ```
     */
    fun getElementToTest(psiElement: PsiElement): PsiElement? {
        val jsFunc = PsiTreeUtil.getParentOfType(psiElement, JSFunction::class.java, false)
        val jsVarStatement = PsiTreeUtil.getParentOfType(psiElement, JSVarStatement::class.java, false)
        val jsClazz = PsiTreeUtil.getParentOfType(psiElement, JSClass::class.java, false)

        val elementForTests: PsiElement? = when {
            jsFunc != null -> jsFunc
            jsVarStatement != null -> jsVarStatement
            jsClazz != null -> jsClazz
            else -> null
        }

        if (elementForTests == null) return null

        return if (isExportedClassPublicMethod(elementForTests) ||
            isExportedFileFunction(elementForTests) ||
            isExportedClass(elementForTests)
        ) {
            elementForTests
        } else {
            null
        }
    }


    companion object {
        fun isExportedFileFunction(element: PsiElement): Boolean {
            val parent = element.parent

            if ((parent is JSFile) || (parent is JSEmbeddedContent)) {
                return when (element) {
                    is JSVarStatement -> {
                        val variables = element.variables
                        val variable = variables.firstOrNull()
                        variable != null && variable.initializerOrStub is JSFunction && exported(variable)
                    }

                    is JSFunction -> exported(element)
                    else -> false
                }
            } else if (parent is JSVariable) {
                val varStatement = parent.parent as? JSVarStatement
                return varStatement != null && varStatement.parent is JSFile && exported(parent)
            } else {
                return parent is ES6ExportDefaultAssignment
            }
        }

        fun isExportedClass(elementForTests: PsiElement?): Boolean {
            return elementForTests is JSClass && elementForTests.isExported
        }

        fun isExportedClassPublicMethod(element: PsiElement): Boolean {
            val jsClass = PsiTreeUtil.getParentOfType(element, JSClass::class.java, true) ?: return false

            if (!exported(jsClass as PsiElement)) return false

            val jsFunction = PsiTreeUtil.getParentOfType(element, JSFunction::class.java, true)
            return jsFunction != null && jsFunction.isExported && !isPrivateMember(jsFunction)
        }

        fun exported(element: PsiElement): Boolean {
            if (element !is JSElementBase) return false

            if (element.isExported || element.isExportedWithDefault) {
                return true
            }

            if (element is JSPsiElementBase && CommonJSUtil.isExportedWithModuleExports(element)) {
                return true
            }

            val containingFile = element.containingFile ?: return false
            val exportDeclarations =
                PsiTreeUtil.getChildrenOfTypeAsList(containingFile, ES6ExportDeclaration::class.java)

            return exportDeclarations.any { exportDeclaration ->
                exportDeclaration.exportSpecifiers
                    .asSequence()
                    .any { it.alias?.findAliasedElement() == element }
            }
        }

        fun elementName(psiElement: PsiElement): String? {
            if (psiElement !is JSVarStatement) {
                if (psiElement !is JSNamedElement) return null

                return psiElement.name
            }

            val jSVariable = psiElement.variables.firstOrNull() ?: return null
            return jSVariable.name
        }

        fun isPrivateMember(element: PsiElement): Boolean {
            if (element is JSQualifiedNamedElement && element.isPrivateName) {
                return true
            }

            if (element !is JSAttributeListOwner) return false

            val attributeList = element.attributeList
            return attributeList?.accessType == JSAttributeList.AccessType.PRIVATE
        }
    }
}
