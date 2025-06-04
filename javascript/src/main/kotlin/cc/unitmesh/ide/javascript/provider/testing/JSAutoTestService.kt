package cc.unitmesh.ide.javascript.provider.testing

import cc.unitmesh.devti.context.ClassContext
import cc.unitmesh.devti.context.ClassContextProvider
import cc.unitmesh.devti.provider.AutoTestService
import cc.unitmesh.devti.provider.context.TestFileContext
import cc.unitmesh.ide.javascript.context.JavaScriptClassContextBuilder
import cc.unitmesh.ide.javascript.context.JavaScriptMethodContextBuilder
import cc.unitmesh.ide.javascript.util.JSPsiUtil
import cc.unitmesh.ide.javascript.util.JSTypeResolver
import cc.unitmesh.ide.javascript.util.LanguageApplicableUtil
import com.intellij.execution.configurations.RunProfile
import com.intellij.lang.javascript.buildTools.npm.rc.NpmRunConfiguration
import com.intellij.lang.javascript.psi.JSFile
import com.intellij.lang.javascript.psi.JSFunction
import com.intellij.lang.javascript.psi.JSVarStatement
import com.intellij.lang.javascript.psi.ecmal4.JSClass
import com.intellij.lang.javascript.psi.ecmal4.JSImportStatement
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import java.io.File
import java.nio.file.Path
import kotlin.io.path.Path

class JSAutoTestService : AutoTestService() {
    private val log = logger<JSAutoTestService>()
    override fun runConfigurationClass(project: Project): Class<out RunProfile> = NpmRunConfiguration::class.java

    override fun isApplicable(element: PsiElement): Boolean {
        val sourceFile = runReadAction { element.containingFile } ?: return false
        return LanguageApplicableUtil.isWebChatCreationContextSupported(sourceFile)
    }

    override fun isApplicable(project: Project, file: VirtualFile): Boolean {
        val psiFile = PsiManager.getInstance(project).findFile(file) as? JSFile ?: return false
        return LanguageApplicableUtil.isWebChatCreationContextSupported(psiFile)
    }

    override fun findOrCreateTestFile(sourceFile: PsiFile, project: Project, psiElement: PsiElement): TestFileContext? {
        val language = sourceFile.language
        val testFilePath = Util.getTestFilePath(psiElement)?.toString()
        if (testFilePath == null) {
            log.warn("Failed to find test file path for: $psiElement")
            return null
        }

        val elementToTest = runReadAction { Util.getElementToTest(psiElement) }
        if (elementToTest == null) {
            log.error("Failed to find element to test for: ${psiElement}, check your function is exported.")
            return null
        }

        val elementName = runReadAction { JSPsiUtil.elementName(elementToTest) }
        if (elementName == null) {
            log.error("Failed to find element name for: $psiElement")
            return null
        }

        var testFile = LocalFileSystem.getInstance().findFileByPath(testFilePath)
        if (testFile != null) {
            return TestFileContext(false, testFile, emptyList(), null, language, null)
        }

        WriteCommandAction.writeCommandAction(sourceFile.project).withName("Generate Unit Tests")
            .compute<Unit, Throwable> {
                val parentDir = VfsUtil.createDirectoryIfMissing(Path(testFilePath).parent.toString())
                testFile = parentDir?.createChildData(this, Path(testFilePath).fileName.toString())
            }

        val underTestObj = ReadAction.compute<String, Throwable> {
            val underTestObj = JavaScriptClassContextBuilder()
                .getClassContext(elementToTest, false)?.format()

            if (underTestObj == null) {
                val funcObj = JavaScriptMethodContextBuilder()
                    .getMethodContext(elementToTest, false, false)?.format()

                return@compute funcObj ?: ""
            } else {
                return@compute underTestObj
            }
        }

        val imports: List<String> = (sourceFile as? JSFile)?.let {
            runReadAction { PsiTreeUtil.findChildrenOfType(it, JSImportStatement::class.java) }
        }?.map {
            it.text
        } ?: emptyList()

        return TestFileContext(true, testFile!!, emptyList(), elementName, language, underTestObj, imports)
    }

    override fun lookupRelevantClass(project: Project, element: PsiElement): List<ClassContext> {
        return JSTypeResolver.resolveByElement(element).map(ClassContextProvider(false)::from)
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
            val testDirectory = suggestTestDirectory(element)
            if (testDirectory == null) {
                log.warn("Failed to find test directory for: $element")
                return null
            }

            val containingFile: PsiFile = runReadAction { element.containingFile } ?: return null
            val extension = containingFile.virtualFile?.extension ?: return null
            val elementName = runReadAction { JSPsiUtil.elementName(element) } ?: return null
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
