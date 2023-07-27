package cc.unitmesh.idea.provider

import cc.unitmesh.devti.context.ClassContext
import cc.unitmesh.devti.context.ClassContextProvider
import cc.unitmesh.devti.provider.TestContextProvider
import cc.unitmesh.devti.provider.TestFileContext
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import com.intellij.psi.impl.source.PsiClassReferenceType

class JavaTestContextProvider : TestContextProvider() {

    override fun findOrCreateTestFile(sourceFile: PsiFile, project: Project, element: PsiElement): TestFileContext? {
        val sourceFilePath = sourceFile.virtualFile
        val sourceDir = sourceFilePath.parent

        val packageName = (sourceFile as PsiJavaFile).packageName

        val relatedModels = lookupRelevantClass(project, element)

        // Check if the source file is in the src/main/java directory
        if (!sourceDir?.path?.contains("/src/main/java/")!!) {
            // Not in the src/main/java directory, return null (cannot find test directory)
            return null
        }

        // Find the test directory
        val testDirPath = sourceDir.path.replace("/src/main/java/", "/src/test/java/")
        val testDir = LocalFileSystem.getInstance().findFileByPath(testDirPath)

        // Check if the test directory exists, if not, create it
        if (testDir == null || !testDir.isDirectory) {
            val testDirCreated = LocalFileSystem.getInstance().refreshAndFindFileByPath(testDirPath)
            return if (testDirCreated != null) {
                // Successfully created the test directory
                val targetFile = createTestFile(sourceFile, testDirCreated, packageName)
                TestFileContext(true, targetFile, relatedModels)
            } else {
                // Failed to create the test directory, return null
                null
            }
        }

        // Test directory already exists, find the corresponding test file
        val testFilePath = testDirPath + "/" + sourceFile.name.replace(".java", "Test.java")
        val testFile = LocalFileSystem.getInstance().findFileByPath(testFilePath)

        return if (testFile != null) {
            TestFileContext(false, testFile, relatedModels)
        } else {
            val targetFile = createTestFile(sourceFile, testDir, packageName)
            TestFileContext(true, targetFile, relatedModels)
        }
    }

    override fun lookupRelevantClass(project: Project, element: PsiElement): List<ClassContext> {
        val models = mutableListOf<ClassContext>()
        val projectPath = project.guessProjectDir()?.path

        val resolvedClasses = resolveByMethod(element)

        if (element is PsiClass) {
            val methods = element.methods
            methods.forEach { method ->
                resolvedClasses.putAll(resolveByMethod(method))
            }
        }

        resolvedClasses.forEach { (_, psiClass) ->
            val classPath = psiClass?.containingFile?.virtualFile?.path
            if (classPath?.contains(projectPath!!) == true) {
                models.add(ClassContextProvider(false).from(psiClass))
            }
        }

        logger<JavaTestContextProvider>().warn("models: $models")
        return models
    }

    private fun resolveByMethod(element: PsiElement): MutableMap<String, PsiClass?> {
        val resolvedClasses = mutableMapOf<String, PsiClass?>()
        if (element is PsiMethod) {
            element.parameterList.parameters.map {
                resolvedClasses[it.name] = (it.type as PsiClassReferenceType).resolve()
            }

            val outputType = element.returnTypeElement?.type
            if (outputType is PsiClassReferenceType) {
                val canonicalText = outputType.canonicalText
                resolvedClasses[canonicalText] = outputType.resolve()
            }
        }

        return resolvedClasses
    }

    override fun insertTestCode(sourceFile: PsiFile, project: Project, methodName: String, code: String): Boolean {
        // Get the root element (usually a class) of the source file
        val rootElement = sourceFile.children.find { it is PsiClass } as? PsiClass

        if (rootElement != null) {
            // Check if a method with the same name already exists
            val existingMethod = rootElement.methods.find { it.name == methodName }

            if (existingMethod != null) {
                // Method with the same name already exists, return an error message
//                return "Error: Method with name '$methodName' already exists."
                return false
            } else {
                // Create the new test method
                val psiElementFactory = PsiElementFactory.getInstance(project)
                val newTestMethod =
                    psiElementFactory.createMethodFromText("void $methodName() {\n$code\n}", rootElement)

                // Add the new method to the class
                val addedMethod = rootElement.add(newTestMethod) as PsiMethod

                // Format the newly inserted code
                addedMethod.navigate(true)

                // Refresh the project to make the changes visible
                project.guessProjectDir()?.refresh(true, true)

//                return "Success: Method '$methodName' successfully added."
                return true
            }
        } else {
//            return "Error: Failed to find the class to insert the method."
            return false
        }
    }

    private fun createTestFile(sourceFile: PsiFile, testDir: VirtualFile, packageName: String): VirtualFile {
        // Create the test file content based on the source file
        val sourceFileName = sourceFile.name
        val testFileName = sourceFileName.replace(".java", "Test.java")
        val testFileContent = """package $packageName;
            |$AUTO_DEV_PLACEHOLDER""".trimMargin()

        // Create the test file in the test directory
        val testFile = testDir.createChildData(this, testFileName)
        testFile.setBinaryContent(testFileContent.toByteArray())

        // Refresh the test directory to make sure the test file is visible
        testDir.refresh(false, true)

        return testFile
    }
}