package cc.unitmesh.ide.javascript.provider.testing

import cc.unitmesh.devti.context.ClassContext
import cc.unitmesh.devti.provider.WriteTestService
import cc.unitmesh.devti.provider.context.TestFileContext
import cc.unitmesh.ide.javascript.util.LanguageApplicableUtil
import cc.unitmesh.ide.javascript.util.JSPsiUtil
import com.intellij.execution.configurations.RunProfile
import com.intellij.lang.javascript.buildTools.npm.rc.NpmRunConfiguration
import com.intellij.lang.javascript.psi.*
import com.intellij.lang.javascript.psi.ecmal4.JSClass
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.util.PsiTreeUtil
import kotlin.io.path.Path
import kotlin.io.path.nameWithoutExtension

class JSWriteTestService : WriteTestService() {
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
        val elementName = JSPsiUtil.elementName(elementToTest) ?: return null

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

        return if (JSPsiUtil.isExportedClassPublicMethod(elementForTests) ||
            JSPsiUtil.isExportedFileFunction(elementForTests) ||
            JSPsiUtil.isExportedClass(elementForTests)
        ) {
            elementForTests
        } else {
            null
        }
    }

}
