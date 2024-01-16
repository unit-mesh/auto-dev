package cc.unitmesh.ide.javascript.provider.testing

import cc.unitmesh.devti.context.ClassContext
import cc.unitmesh.devti.provider.WriteTestService
import cc.unitmesh.devti.provider.context.TestFileContext
import cc.unitmesh.ide.javascript.context.JavaScriptClassContextBuilder
import cc.unitmesh.ide.javascript.util.LanguageApplicableUtil
import cc.unitmesh.ide.javascript.util.JSPsiUtil
import com.intellij.execution.configurations.RunProfile
import com.intellij.lang.javascript.buildTools.npm.rc.NpmRunConfiguration
import com.intellij.lang.javascript.psi.*
import com.intellij.lang.javascript.psi.ecmal4.JSClass
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findDirectory
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import java.io.File
import java.io.IOException
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.nameWithoutExtension

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

        val testFile = LocalFileSystem.getInstance().findFileByPath(testFilePath)
        if (testFile != null) {
            return TestFileContext(false, testFile, emptyList(), null, language, null)
        }

        val testFileName = Path(testFilePath).nameWithoutExtension
        val testFileText = ""
        val testFilePsi = ReadAction.compute<PsiFile, Throwable> {
            PsiFileFactory.getInstance(project).createFileFromText(testFileName, language, testFileText)
        }

        // write test file
        WriteCommandAction.runWriteCommandAction(project) {
            val document = testFilePsi.viewProvider.document!!
            document.insertString(document.textLength, testFileText)
            FileDocumentManager.getInstance().saveDocument(document)
        }

        val currentClz = JavaScriptClassContextBuilder().getClassContext(elementToTest, false)

        return TestFileContext(true, testFilePsi.virtualFile, emptyList(), elementName, language, currentClz)
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
        private fun suggestTestDirectory(element: PsiElement): PsiDirectory? {
            val project: Project = element.project
            val elementDirectory = runReadAction { element.containingFile }

            val parentDir = elementDirectory?.virtualFile?.parent ?: return null
            val psiManager = PsiManager.getInstance(project)

            val testDir = runReadAction { parentDir.findDirectory("test") }
            testDir?.let {
                return runReadAction { psiManager.findDirectory(it) }
            }

            val outputFile = WriteCommandAction.writeCommandAction(project, elementDirectory)
                .withName("Creating Directory For Tests")
                .compute<VirtualFile?, IOException> {
                    return@compute parentDir.createChildDirectory(this, "test")
                } ?: return null

            return runReadAction { psiManager.findDirectory(outputFile) }
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
