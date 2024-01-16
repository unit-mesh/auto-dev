package cc.unitmesh.ide.javascript.provider.testing

import cc.unitmesh.devti.context.ClassContext
import cc.unitmesh.devti.provider.WriteTestService
import cc.unitmesh.devti.provider.context.TestFileContext
import cc.unitmesh.ide.javascript.context.JavaScriptClassContextBuilder
import cc.unitmesh.ide.javascript.util.JSPsiUtil
import cc.unitmesh.ide.javascript.util.LanguageApplicableUtil
import com.intellij.execution.configurations.RunProfile
import com.intellij.lang.javascript.buildTools.npm.rc.NpmRunConfiguration
import com.intellij.lang.javascript.psi.JSFunction
import com.intellij.lang.javascript.psi.JSVarStatement
import com.intellij.lang.javascript.psi.ecmal4.JSClass
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import java.io.File
import java.nio.file.Path
import kotlin.io.path.Path

class JSWriteTestService : WriteTestService() {
    override fun runConfigurationClass(project: Project): Class<out RunProfile> = NpmRunConfiguration::class.java

    override fun isApplicable(element: PsiElement): Boolean {
        val sourceFile: PsiFile = element.containingFile ?: return false
        return LanguageApplicableUtil.isWebChatCreationContextSupported(sourceFile)
    }

    override fun findOrCreateTestFile(sourceFile: PsiFile, project: Project, element: PsiElement): TestFileContext? {
        val language = sourceFile.language
        val testFilePath = Util.getTestFilePath(element)?.toString() ?: return null

        val elementToTest = Util.getElementToTest(element) ?: return null
        val elementName = JSPsiUtil.elementName(elementToTest) ?: return null

        var testFile = LocalFileSystem.getInstance().findFileByPath(testFilePath)
        if (testFile != null) {
            return TestFileContext(false, testFile, emptyList(), null, language, null)
        }

        // create test file


        WriteCommandAction.writeCommandAction(sourceFile.project).withName("Generate Unit Tests")
            .compute<Unit, Throwable> {
                val parentDir = VfsUtil.createDirectoryIfMissing(Path(testFilePath).parent.toString())
                testFile = parentDir?.createChildData(this, Path(testFilePath).fileName.toString())
            }

        val currentClz = JavaScriptClassContextBuilder().getClassContext(elementToTest, false)

        return TestFileContext(true, testFile!!, emptyList(), elementName, language, currentClz)
    }

    override fun lookupRelevantClass(project: Project, element: PsiElement): List<ClassContext> {
        return ReadAction.compute<List<ClassContext>, Throwable> {
            val elements = mutableListOf<ClassContext>()
            val projectPath = project.guessProjectDir()?.path

            when (element) {
                is JSClass -> {
                    element.functions.map {
                        resolveByFunction(it)
                    }
                }

                is JSFunction -> {
                    resolveByFunction(element)
                }
            }

            return@compute listOf()
        }
    }

    private fun resolveByFunction(jsFunction: JSFunction): Map<String, ClassContext> {
        jsFunction.parameterList?.parameters?.map {

        }

        val resolveClass = jsFunction.returnTypeElement
        return mapOf()
    }

    object Util {

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

            return when {
                JSPsiUtil.isExportedClassPublicMethod(elementForTests) -> elementForTests
                JSPsiUtil.isExportedFileFunction(elementForTests) -> elementForTests
                JSPsiUtil.isExportedClass(elementForTests) -> elementForTests
                else -> {
                    null
                }
            }
        }

        fun getTestFilePath(element: PsiElement): Path? {
            val testDirectory = suggestTestDirectory(element) ?: return null
            val containingFile: PsiFile = runReadAction { element.containingFile } ?: return null
            val extension = containingFile.virtualFile?.extension ?: return null
            val elementName = JSPsiUtil.elementName(element) ?: return null
            val testFile: Path = generateUniqueTestFile(elementName, containingFile, testDirectory, extension).toPath()
            return testFile
        }

        /**
         * Todo: since in JavaScript has different test framework, we need to find the test directory by the framework.
         */
        private fun suggestTestDirectory(element: PsiElement): PsiDirectory? =
            ReadAction.compute<PsiDirectory?, Throwable> {
                val project: Project = element.project
                val elementDirectory = element.containingFile

                val parentDir = elementDirectory?.virtualFile?.parent ?: return@compute null
                val psiManager = PsiManager.getInstance(project)

                val findDirectory = psiManager.findDirectory(parentDir)
                if (findDirectory != null) {
                    return@compute findDirectory
                }

                val createChildDirectory = parentDir.createChildDirectory(this, "test")
                return@compute psiManager.findDirectory(createChildDirectory)
            }

        private fun generateUniqueTestFile(
            elementName: String?,
            containingFile: PsiFile,
            testDirectory: PsiDirectory,
            extension: String
        ): File {
            val testPath = testDirectory.virtualFile.path
            val prefix = elementName ?: containingFile.name.substringBefore('.', "")
            val nameCandidate = "$prefix.test.$extension"
            var testFile = File(testPath, nameCandidate)

            var i = 1
            while (testFile.exists()) {
                val nameCandidateWithIndex = "$prefix${i}.test.$extension"
                i++
                testFile = File(testPath, nameCandidateWithIndex)
            }

            return testFile
        }
    }
}
