package cc.unitmesh.kotlin.provider

import cc.unitmesh.devti.context.ClassContext
import cc.unitmesh.devti.context.ClassContextProvider
import cc.unitmesh.devti.context.FileContextProvider
import cc.unitmesh.devti.provider.context.TestFileContext
import cc.unitmesh.devti.provider.AutoTestService
import cc.unitmesh.idea.service.createConfigForJava
import cc.unitmesh.kotlin.util.KotlinPsiUtil
import cc.unitmesh.kotlin.util.KotlinTypeResolver
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerEx
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunProfile
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.messages.MessageBusConnection
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.codeInsight.KotlinReferenceImporter
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getValueParameters
import org.jetbrains.plugins.gradle.service.execution.GradleRunConfiguration
import java.io.File

class KotlinAutoTestService : AutoTestService() {
    private val log = logger<KotlinAutoTestService>()
    override fun runConfigurationClass(project: Project): Class<out RunProfile> = GradleRunConfiguration::class.java
    override fun isApplicable(element: PsiElement): Boolean = element.language is KotlinLanguage
    override fun isApplicable(project: Project, file: VirtualFile): Boolean {
        return file.extension == "kt"
    }

    override fun createConfiguration(project: Project, virtualFile: VirtualFile): RunConfiguration? {
        return createConfigForJava(virtualFile, project)
    }

    override fun findOrCreateTestFile(sourceFile: PsiFile, project: Project, psiElement: PsiElement): TestFileContext? {
        val sourceFilePath = sourceFile.virtualFile
        val parentDir = sourceFilePath.parent
        val className = sourceFile.name.replace(".kt", "") + "Test"

        val parentDirPath = ReadAction.compute<String, Throwable> {
            parentDir?.path
        } ?: return null

        val relatedModels = lookupRelevantClass(project, psiElement).distinctBy { it.name }

        if (!(parentDirPath.contains("/main/java") || parentDirPath.contains("/main/kotlin"))) {
            log.error("SourceFile is not under the main/kotlin or main/java directory: $parentDirPath")
            return null
        }

        var isNewFile = false

        val testDirPath = parentDir.path
            .replace("/main/kotlin/", "/test/kotlin/")
            .replace("/main/java/", "/test/java/")

        var testDir = LocalFileSystem.getInstance().findFileByPath(testDirPath)

        if (testDir == null || !testDir.isDirectory) {
            isNewFile = true
            // Create the test directory if it doesn't exist
            val testDirFile = File(testDirPath)
            if (!testDirFile.exists()) {
                testDirFile.mkdirs()

                LocalFileSystem.getInstance().refreshAndFindFileByPath(testDirPath)?.let { refreshedDir ->
                    testDir = refreshedDir
                }
            }
        }

        val testDirCreated: VirtualFile? =
            VirtualFileManager.getInstance().refreshAndFindFileByUrl("file://$testDirPath")
        if (testDirCreated == null) {
            log.error("Failed to create test directory: $testDirPath")
            return null
        }

        // Test directory already exists, find the corresponding test file
        val testFilePath = testDirPath + "/" + sourceFile.name.replace(".kt", "Test.kt")
        val testFile = LocalFileSystem.getInstance().findFileByPath(testFilePath)

        project.guessProjectDir()?.refresh(true, true)

        val currentClass: String = ReadAction.compute<String, Throwable> {
            val classContext = when (psiElement) {
                is KtFile -> FileContextProvider().from(psiElement)
                is KtClassOrObject -> ClassContextProvider(false).from(psiElement)
                is KtNamedFunction -> {
                    PsiTreeUtil.getParentOfType(psiElement, KtClassOrObject::class.java)?.let {
                        ClassContextProvider(false).from(it)
                    }
                }

                else -> null
            }

            return@compute classContext?.format() ?: ""
        }

        val imports: List<String> = runReadAction {
            (sourceFile as KtFile).importList?.imports?.map { it.text } ?: emptyList()
        }

        return if (testFile != null) {
            TestFileContext(isNewFile, testFile, relatedModels, className, sourceFile.language, currentClass, imports)
        } else {
            val targetFile = createTestFile(sourceFile, testDir!!, project)
            TestFileContext(isNewFile = true, targetFile, relatedModels, "", sourceFile.language, currentClass, imports)
        }
    }

    override fun lookupRelevantClass(project: Project, element: PsiElement): List<ClassContext> {
        return ReadAction.compute<List<ClassContext>, Throwable> {
            val elements = mutableListOf<ClassContext>()
            val projectPath = project.guessProjectDir()?.path

            val resolvedClasses = KotlinTypeResolver.resolveByMethod(element)

            if (element is KtClassOrObject) {
                KotlinPsiUtil.getFunctions(element).forEach {
                    resolvedClasses.putAll(KotlinTypeResolver.resolveByMethod(it))
                }

                resolvedClasses.putAll(KotlinTypeResolver.resolveByFields(element))
            }

            if (element is KtFile) {
                KotlinPsiUtil.getClasses(element).forEach {
                    resolvedClasses.putAll(KotlinTypeResolver.resolveByFields(it))
                }
            }


            // find the class in the same project
            resolvedClasses.forEach { (_, psiClass) ->
                val classPath = psiClass?.containingFile?.virtualFile?.path
                if (classPath?.contains(projectPath!!) == true) {
                    elements += ClassContextProvider(false).from(psiClass)
                }
            }

            elements
        }
    }

    private fun createTestFile(
        sourceFile: PsiFile,
        testDir: VirtualFile,
        project: Project
    ): VirtualFile {
        val sourceFileName = sourceFile.name
        val testFileName = sourceFileName.replace(".kt", "Test.kt")
        val testFileContent = ""

        return WriteCommandAction.runWriteCommandAction<VirtualFile>(project) {
            val testFile = testDir.createChildData(this, testFileName)

            val document = FileDocumentManager.getInstance().getDocument(testFile)
            document?.setText(testFileContent)
            testFile
        }
    }

    override fun collectSyntaxError(
        outputFile: VirtualFile,
        project: Project,
        runAction: ((errors: List<String>) -> Unit)?
    ) {
        val sourceFile: KtFile =
            runReadAction { PsiManager.getInstance(project).findFile(outputFile) as? KtFile } ?: return
        val collectPsiError = sourceFile.collectPsiError()
        if (collectPsiError.isNotEmpty()) {
            runAction?.invoke(collectPsiError)
            return
        }

        val document = runReadAction { FileDocumentManager.getInstance().getDocument(outputFile) } ?: return
        val range = TextRange(0, document.textLength)

        DaemonCodeAnalyzerEx.getInstance(project).restart(sourceFile);

        val hintDisposable = Disposer.newDisposable()
        val busConnection: MessageBusConnection = project.messageBus.connect(hintDisposable)

        class DaemonListener : DaemonCodeAnalyzer.DaemonListener {
            val errors = mutableListOf<String>()

            override fun daemonFinished() {
                DaemonCodeAnalyzerEx.processHighlights(
                    document,
                    project,
                    HighlightSeverity.ERROR,
                    range.startOffset,
                    range.endOffset
                ) {
                    if (it.description != null) {
                        errors.add(it.description)
                    }

                    true
                }

                runAction?.invoke(errors)
                busConnection.disconnect()
                Disposer.dispose(hintDisposable)
            }
        }

        busConnection.subscribe<DaemonCodeAnalyzer.DaemonListener>(
            DaemonCodeAnalyzer.DAEMON_EVENT_TOPIC,
            DaemonListener()
        )
    }

    override fun tryFixSyntaxError(outputFile: VirtualFile, project: Project, issues: List<String>) {
        val sourceFile = runReadAction {
            PsiManager.getInstance(project).findFile(outputFile) as? KtFile
        } ?: return

        val editor = FileEditorManager.getInstance(project).selectedTextEditor ?: return
        val importer = KotlinReferenceImporter()
        importer.autoImportReferenceAtCursor(editor, sourceFile)
    }
}
