package cc.unitmesh.idea.service

import cc.unitmesh.devti.context.ClassContext
import cc.unitmesh.devti.context.ClassContextProvider
import cc.unitmesh.devti.provider.AutoTestService
import cc.unitmesh.devti.provider.context.TestFileContext
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerEx
import com.intellij.execution.RunManager
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunProfile
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.messages.MessageBusConnection
import org.jetbrains.idea.maven.execution.MavenRunConfiguration
import org.jetbrains.idea.maven.execution.MavenRunConfigurationType
import org.jetbrains.idea.maven.execution.MavenRunnerParameters
import org.jetbrains.idea.maven.project.MavenProject
import org.jetbrains.idea.maven.project.MavenProjectsManager
import org.jetbrains.plugins.gradle.service.execution.GradleExternalTaskConfigurationType
import org.jetbrains.plugins.gradle.service.execution.GradleRunConfiguration
import org.jetbrains.plugins.gradle.util.GradleConstants
import java.io.File
import java.util.concurrent.CompletableFuture

class JavaAutoTestService : AutoTestService() {
    private val maxLevelOneClass = 8
    override fun runConfigurationClass(project: Project): Class<out RunProfile> = GradleRunConfiguration::class.java
    override fun isApplicable(element: PsiElement): Boolean = element.language is JavaLanguage
    override fun isApplicable(project: Project, file: VirtualFile): Boolean {
        return file.extension == "java"
    }

    override fun createConfiguration(project: Project, virtualFile: VirtualFile): RunConfiguration? {
        val psiFile =
            runReadAction { PsiManager.getInstance(project).findFile(virtualFile) } as? PsiJavaFile ?: return null

        if (psiFile.collectPsiError().isNotEmpty()) {
            return null
        }

        return createConfigForJava(virtualFile, project)
    }

    override fun findOrCreateTestFile(sourceFile: PsiFile, project: Project, psiElement: PsiElement): TestFileContext? {
        val sourceFilePath = sourceFile.virtualFile
        val parentDir = sourceFilePath.parent
        val testFileName = sourceFile.name.replace(".java", "") + "Test"

        val parentDirPath = ReadAction.compute<String, Throwable> { parentDir?.path }

        val relatedModels = lookupRelevantClass(project, psiElement).distinctBy { it.name }

        // Check if the source file is in the src/main/java directory
        if (!parentDirPath?.contains("/src/main/java/")!!) {
            log.error("Source file is not in the src/main/java directory: $parentDirPath")
            return null
        }

        var isNewFile = false

        // Find the test directory
        val testDirPath = parentDirPath.replace("/src/main/java/", "/src/test/java/")
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
        val testFilePath = testDirPath + "/" + sourceFile.name.replace(".java", "Test.java")
        val testFile = LocalFileSystem.getInstance().findFileByPath(testFilePath)

        project.guessProjectDir()?.refresh(true, true)

        val imports = runReadAction {
            val importList = PsiTreeUtil.getChildrenOfTypeAsList(sourceFile, PsiImportList::class.java)
            importList.flatMap { it.allImportStatements.map { import -> import.text } }
        }

        val currentClass = extracted(psiElement)

        return if (testFile != null) {
            TestFileContext(
                isNewFile,
                testFile,
                relatedModels,
                testFileName,
                sourceFile.language,
                currentClass,
                imports
            )
        } else {
            val targetFile = createTestFile(sourceFile, testDir!!, project)
            TestFileContext(isNewFile = true, targetFile, relatedModels, "", sourceFile.language, currentClass, imports)
        }
    }

    private fun extracted(psiElement: PsiElement): String? {
        var currentClass: ClassContext? = null;

        val classContextProvider = ClassContextProvider(false)
        when (psiElement) {
            is PsiJavaFile -> {
                currentClass = runReadAction { psiElement.classes.firstOrNull()?.let { classContextProvider.from(it) } }
            }

            is PsiClass -> {
                currentClass = runReadAction { classContextProvider.from(psiElement) }
            }

            is PsiMethod -> {
                currentClass = runReadAction { psiElement.containingClass?.let { classContextProvider.from(it) } }
            }
        }

        return currentClass?.format();
    }

    override fun lookupRelevantClass(project: Project, element: PsiElement): List<ClassContext> {
        return ReadAction.compute<List<ClassContext>, Throwable> {
            val elements = mutableListOf<ClassContext>()
            val projectPath = project.guessProjectDir()?.path

            val resolvedClasses: MutableMap<String, PsiClass> = mutableMapOf()
            resolvedClasses.putAll(JavaTypeUtil.resolveByField(element))

            when (element) {
                is PsiJavaFile -> {
                    element.classes.forEach { psiClass ->
                        resolvedClasses.putAll(JavaTypeUtil.resolveByClass(psiClass))
                    }
                }

                is PsiClass -> {
                    element.methods.forEach { method ->
                        resolvedClasses.putAll(JavaTypeUtil.resolveByMethod(method))
                    }
                }

                is PsiMethod -> {
                    resolvedClasses.putAll(JavaTypeUtil.resolveByMethod(element))
                }
            }

            if (resolvedClasses.isEmpty()) {
                return@compute elements
            }

            if ((resolvedClasses.size <= maxLevelOneClass) || element is PsiMethod) {
                // load all second childrens
                val childClasses: MutableMap<String, PsiClass> = mutableMapOf()
                resolvedClasses.forEach { (key, value) ->
                    value.fields.forEach { field ->
                        childClasses.putAll(JavaTypeUtil.resolveByType(field.type))
                    }
                }

                resolvedClasses.putAll(childClasses)
            }

            // find the class in the same project
            resolvedClasses.forEach { (_, psiClass) ->
                val classPath = psiClass.containingFile?.virtualFile?.path
                if (classPath?.contains(projectPath!!) == true) {
                    elements += ClassContextProvider(false).from(psiClass)
                }
            }

            elements
        }
    }

    override fun tryFixSyntaxError(outputFile: VirtualFile, project: Project, issues: List<String>) {
        val sourceFile: PsiJavaFile =
            runReadAction { PsiManager.getInstance(project).findFile(outputFile) as? PsiJavaFile } ?: return

        val editor = FileEditorManager.getInstance(project).selectedTextEditor ?: return
        DaemonCodeAnalyzer.getInstance(project).autoImportReferenceAtCursor(editor, sourceFile)
    }

    override fun collectSyntaxError(
        outputFile: VirtualFile,
        project: Project,
        runAction: ((errors: List<String>) -> Unit)?
    ) {
        val future: CompletableFuture<Unit> = CompletableFuture()

        val sourceFile =
            runReadAction { PsiManager.getInstance(project).findFile(outputFile) as? PsiJavaFile } ?: return
        val collectPsiError = sourceFile.collectPsiError()
        if (collectPsiError.isNotEmpty()) {
            runAction?.invoke(collectPsiError)
            return
        }

        val document = runReadAction { FileDocumentManager.getInstance().getDocument(outputFile) } ?: return
        val range = TextRange(0, document.textLength)
        val errors = mutableListOf<String>()

        DaemonCodeAnalyzerEx.getInstance(project).restart(sourceFile);

        val hintDisposable = Disposer.newDisposable()
        val busConnection: MessageBusConnection = project.messageBus.connect(hintDisposable)
        busConnection.subscribe<DaemonCodeAnalyzer.DaemonListener>(
            DaemonCodeAnalyzer.DAEMON_EVENT_TOPIC,
            object : DaemonCodeAnalyzer.DaemonListener {
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

                    future.complete(null)
                    runAction?.invoke(errors)
                    busConnection.disconnect()
                    Disposer.dispose(hintDisposable)
                }
            })
    }

    private fun createTestFile(sourceFile: PsiFile, testDir: VirtualFile, project: Project): VirtualFile {
        val sourceFileName = sourceFile.name
        val testFileName = sourceFileName.replace(".java", "Test.java")
        val testFileContent = ""

        return WriteCommandAction.runWriteCommandAction<VirtualFile>(project) {
            val testFile = testDir.createChildData(this, testFileName)
            val document = FileDocumentManager.getInstance().getDocument(testFile)
            document?.setText(testFileContent)
            testFile
        }
    }

    companion object {
        private val log = logger<JavaAutoTestService>()
    }
}

fun createConfigForJava(virtualFile: VirtualFile, project: Project): RunConfiguration? {
    val gradleLibraryData = ProjectDataManager.getInstance().getExternalProjectData(
        project, GradleConstants.SYSTEM_ID, project.basePath!!
    )

    if (gradleLibraryData == null) {
        return createConfigForMaven(virtualFile, project)
    }

    return createConfigForGradle(virtualFile, project)
}

fun createConfigForGradle(virtualFile: VirtualFile, project: Project): GradleRunConfiguration? {
    val name = virtualFile.name

    val canonicalName = runReadAction {
        val psiFile: PsiJavaFile =
            PsiManager.getInstance(project).findFile(virtualFile) as? PsiJavaFile ?: return@runReadAction null
        // skip for non-test files
        (psiFile.packageName + "." + virtualFile.nameWithoutExtension).removePrefix(".")
    } ?: return null

    val runManager = RunManager.getInstance(project)

    var moduleName = ""
    val moduleForFile = runReadAction { ProjectFileIndex.getInstance(project).getModuleForFile(virtualFile) }
    // a moduleForFile.name will be like <project>.<module>.<testModule>, so we need to remove the last part and first part
    if (moduleForFile != null) {
        val moduleNameSplit = moduleForFile.name.split(".").drop(1).dropLast(1).joinToString(":")
        if (moduleNameSplit.isNotEmpty()) {
            moduleName = "$moduleNameSplit:"
        }
    }

    // todo: add maven ??
    val configuration = runManager.createConfiguration(name, GradleExternalTaskConfigurationType::class.java)
    val runConfiguration = configuration.configuration as GradleRunConfiguration

    runConfiguration.isDebugServerProcess = false
    runConfiguration.settings.externalProjectPath = project.guessProjectDir()?.path
    // todo: add module for test
    runConfiguration.rawCommandLine = moduleName + "test --tests \"${canonicalName}\""

    runManager.addConfiguration(configuration)
    runManager.selectedConfiguration = configuration

    return runConfiguration
}

fun createConfigForMaven(virtualFile: VirtualFile, project: Project): MavenRunConfiguration? {
    val projectsManager = MavenProjectsManager.getInstance(project);

    val mavenProject: MavenProject = projectsManager.findProject(virtualFile) ?: return null
    val module = runReadAction { projectsManager.findModule(mavenProject) } ?: return null

    var trulyMavenProject = projectsManager.projects.firstOrNull {
        it.mavenId.artifactId == module.name
    }

    if (trulyMavenProject == null) {
        trulyMavenProject = projectsManager.projects.first() ?: return null
    }

    val pomFile = trulyMavenProject.file.name

    val parameters = MavenRunnerParameters(
        true, trulyMavenProject.directory, pomFile, listOf("test"),
        projectsManager.explicitProfiles.enabledProfiles, arrayListOf()
    )

    // $MODULE_WORKING_DIR$
    //
    // -ea Method: com.example.demo.MathHelperTest should_ReturnSum_When_GivenTwoPositiveNumbers
    // /Users/phodal/Library/Java/JavaVirtualMachines/corretto-18.0.2/Contents/Home/bin/java
    // -ea -Didea.test.cyclic.buffer.size=1048576
    // -javaagent:ideaIU-2024.1/lib/idea_rt.jar=54637:1/bin -Dfile.encoding=UTF-8
    // -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8

    val runnerAndConfigurationSettings =
        MavenRunConfigurationType.createRunnerAndConfigurationSettings(null, null, parameters, project)

    val runManager = RunManager.getInstance(project)

    val configuration = runnerAndConfigurationSettings.configuration

    runManager.addConfiguration(runnerAndConfigurationSettings)
    runManager.selectedConfiguration = runnerAndConfigurationSettings

    return configuration as MavenRunConfiguration
}
