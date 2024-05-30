package cc.unitmesh.idea.spring

import cc.unitmesh.devti.context.model.DtClass
import cc.unitmesh.idea.flow.JavaTemplateHelper
import cc.unitmesh.idea.fromJavaFile
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileSystem
import com.intellij.psi.*
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.IncorrectOperationException
import kotlin.reflect.KFunction1


@Service(Service.Level.PROJECT)
class JavaSpringCodeCreator(val project: Project) : SpringBaseCrud {
    val psiElementFactory: PsiElementFactory get() = JavaPsiFacade.getElementFactory(project)

    private val codeTemplate = JavaTemplateHelper(project)
    private val psiManager = PsiManager.getInstance(project)
    private val searchScope: GlobalSearchScope = ProjectScope.getProjectScope(project)

    override fun getAllControllerFiles(): List<PsiFile> = filterFilesByFunc(::controllerFilter)
    override fun getAllEntityFiles(): List<PsiFile> = filterFilesByFunc(::entityFilter)
    override fun getAllDtoFiles(): List<PsiFile> = filterFilesByFunc(::dtoFilter)
    override fun getAllServiceFiles(): List<PsiFile> = filterFilesByFunc(::serviceFilter)
    override fun getAllRepositoryFiles(): List<PsiFile> = filterFilesByFunc(::repositoryFilter)
    fun getAllModelFiles(): List<DtClass> {
        val files = this.getAllEntityFiles() + this.getAllDtoFiles()
        return files.map(DtClass.Companion::fromJavaFile)
    }

    private fun filterFilesByFunc(filter: KFunction1<PsiClass, Boolean>): List<PsiFile> = runReadAction {
        val javaFiles = FileTypeIndex.getFiles(JavaFileType.INSTANCE, searchScope)
        return@runReadAction filterFiles(javaFiles, psiManager, filter)
    }

    private fun filterFiles(
        javaFiles: Collection<VirtualFile>,
        psiManager: PsiManager,
        filter: (PsiClass) -> Boolean,
    ): List<PsiFile> {
        return runReadAction {
            return@runReadAction javaFiles
                .mapNotNull { virtualFile -> psiManager.findFile(virtualFile) }
                .filter { psiFile ->
                    val psiClass = PsiTreeUtil.findChildrenOfType(psiFile, PsiClass::class.java)
                        .firstOrNull()
                    psiClass != null && filter(psiClass)
                }
        }
    }

    override fun createControllerOrUpdateMethod(targetController: String, code: String, isControllerExist: Boolean) {
        if (!isControllerExist) {
            this.createController(targetController, code)
            return
        }

        val allControllerFiles = getAllControllerFiles()
        if (allControllerFiles.isEmpty()) return

        val targetControllerFile = allControllerFiles.firstOrNull { it.name == "$targetController.java" } ?: return

        updateCodeMethod(targetControllerFile, targetController, code, project, psiElementFactory)
    }

    override fun updateMethod(targetFile: PsiFile, targetClass: String, code: String) {
        updateCodeMethod(targetFile, targetClass, code, project, psiElementFactory)
    }

    override fun createController(endpoint: String, code: String): DtClass? {
        if (getAllControllerFiles().isEmpty()) {
            return DtClass("", emptyList())
        }

        val randomController = firstController()
        val packageStatement = randomController?.lookupPackageName()

        if (packageStatement == null) {
            log.warn("No package statement found in file ${randomController?.name}")
            return DtClass("", emptyList())
        }

        val templateCode = codeTemplate.controller(endpoint, code, packageStatement.packageName)

        val parentDirectory = randomController.virtualFile?.parent ?: return null
        val fileSystem = randomController.virtualFile?.fileSystem

        createClassByTemplate(parentDirectory, fileSystem, endpoint, templateCode)

        return DtClass(endpoint, emptyList())
    }

    override fun createEntity(code: String): DtClass? = createClassByCode(code, getAllEntityFiles(), "model")
    override fun createDto(code: String): DtClass? = createClassByCode(code, getAllDtoFiles(), "dto")
    override fun createRepository(code: String): DtClass? =
        createClassByCode(code, getAllRepositoryFiles(), "repository")

    override fun createService(code: String): DtClass? = createClassByCode(code, getAllServiceFiles(), "service")

    private fun createClassByCode(code: String, psiFiles: List<PsiFile>, layerName: String): DtClass? {
        // REMOVE PREFIX ```JAVA from code
        val fixedCode = code.removePrefix("```java")
            .removePrefix("```JAVA")
            .removeSuffix("```")
            .trim()

        val packageName = if (psiFiles.isNotEmpty()) {
            psiFiles.firstOrNull()?.lookupPackageName()?.packageName
        } else {
            packageCloseToController(layerName)
        }

        val newCode = "package $packageName;\n\n$fixedCode"

        if (packageName == null) {
            log.warn("No package statement found in file ${psiFiles.firstOrNull()?.name}")
            return DtClass("", emptyList())
        }

        return createClass(newCode, packageName)
    }

    override fun createClass(code: String, packageName: String?): DtClass? {
        // controller parent will be ${package}.controller, ${package}.controller parent will be ${package}
        var parentDirectory = firstController()?.virtualFile?.parent?.parent ?: return null
        val fileSystem = firstController()?.virtualFile?.fileSystem
        ApplicationManager.getApplication().invokeLater {
            runWriteAction {
                val newClass = psiElementFactory.createClassFromText(code, null)

                val regex = Regex("public\\s+(class|interface)\\s+(\\w+)")
                val matchResult = regex.find(code)

                val className = if (matchResult?.groupValues?.get(2) != null) {
                    matchResult.groupValues[2]
                } else if (newClass.identifyingElement?.text != null) {
                    newClass.identifyingElement?.text
                } else {
                    "DummyClass"
                }

                val classDir = packageName?.split(".")?.lastOrNull()
                parentDirectory = if (classDir != null) {
                    if (parentDirectory.findChild(classDir) == null) {
                        parentDirectory.createChildDirectory(fileSystem, classDir)
                    } else {
                        parentDirectory.findChild(classDir)!!
                    }
                } else {
                    parentDirectory
                }

                val targetClass = "$className.java"
                if (parentDirectory.findChild(targetClass) != null) {
                    log.info("File $targetClass already exists")
                    return@runWriteAction
                }

                val virtualFile = parentDirectory.createChildData(fileSystem, targetClass)
                VfsUtil.saveText(virtualFile, code)

                log.info("Created file ${virtualFile.path}")
                parentDirectory.refresh(false, true)
            }
        }

        return null
    }

    private fun packageCloseToController(subpackage: String): String? {
        val firstControllerPkg = firstController()?.lookupPackageName()?.packageName
        if (firstControllerPkg != null) {
            val lastDotIndex = firstControllerPkg.lastIndexOf(".")
            if (lastDotIndex != -1) {
                return firstControllerPkg.substring(0, lastDotIndex) + "." + subpackage
            }
        }

        return firstControllerPkg
    }

    private fun firstController(): PsiFile? = getAllControllerFiles().firstOrNull()

    private fun createClassByTemplate(
        parentDirectory: VirtualFile,
        fileSystem: VirtualFileSystem?,
        serviceName: String,
        templateCode: String,
    ) {
        ApplicationManager.getApplication().invokeLater {
            runWriteAction {
                val virtualFile = parentDirectory.createChildData(fileSystem, "$serviceName.java")
                VfsUtil.saveText(virtualFile, templateCode)

                log.info("Created file ${virtualFile.path}")
                parentDirectory.refresh(false, true)
            }
        }
    }

    companion object {
        private val log: Logger = logger<JavaSpringCodeCreator>()
        fun addMethodToClass(psiClass: PsiClass, method: String, elementFactory: PsiElementFactory): PsiClass {
            val methodFromText = elementFactory.createMethodFromText(method, psiClass)
            var lastMethod: PsiMethod? = null
            val allMethods = psiClass.methods

            if (allMethods.isNotEmpty()) {
                lastMethod = allMethods[allMethods.size - 1]
            }

            if (lastMethod != null) {
                psiClass.addAfter(methodFromText, lastMethod)
            } else {
                psiClass.add(methodFromText)
            }

            return psiClass
        }

        fun updateCodeMethod(
            targetFile: PsiFile,
            targetClass: String,
            code: String,
            project: Project,
            elementFactory: PsiElementFactory
        ) {
            ApplicationManager.getApplication().invokeLater {
                val targetControllerClass = PsiTreeUtil.findChildrenOfType(targetFile, PsiClass::class.java)
                    .firstOrNull() ?: return@invokeLater // Return from the lambda if the class is not found

                var method = code
                if (code.contains("class $targetClass")) {
                    method = code.substring(code.indexOf("{") + 1, code.lastIndexOf("}"))
                }

                method = method.trimIndent()

                ApplicationManager.getApplication().invokeLater {
                    WriteCommandAction.writeCommandAction(project)
                        .run<RuntimeException> {
                            try {
                                addMethodToClass(targetControllerClass, method, elementFactory)
                            } catch (e: IncorrectOperationException) {
                                log.warn("Failed to add method to class $targetClass")
                            }
                            CodeStyleManager.getInstance(project).reformat(targetFile)
                        }
                }
            }
        }
    }
}

fun PsiFile.lookupPackageName(): PsiPackageStatement? {
    val packageStatement = runReadAction {
        PsiTreeUtil.findChildrenOfType(this, PsiPackageStatement::class.java)
            .firstOrNull() ?: return@runReadAction null
    }
    return packageStatement
}
