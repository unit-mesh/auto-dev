package cc.unitmesh.devti.flow

import cc.unitmesh.devti.analysis.DtClass
import cc.unitmesh.devti.analysis.fromPsiFile
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileSystem
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiPackageStatement
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.util.PsiTreeUtil


class JavaSpringBaseCrud(val project: Project) : SpringBaseCrud {
    private val psiElementFactory = JavaPsiFacade.getElementFactory(project)
    private val codeTemplate = JavaCrudTemplate(project)

    private val controllers = getAllControllerFiles()
    private val services = getAllServiceFiles()
    private val dto = getAllDtoFiles()
    private val entities = getAllEntityFiles()

    private fun getAllEntityFiles(): List<PsiFile> {
        val psiManager = PsiManager.getInstance(project)

        val searchScope: GlobalSearchScope = ProjectScope.getContentScope(project)
        val javaFiles = FileTypeIndex.getFiles(JavaFileType.INSTANCE, searchScope)

        return filterFiles(javaFiles, psiManager, ::entityFilter)
    }

    private fun getAllControllerFiles(): List<PsiFile> {
        val psiManager = PsiManager.getInstance(project)

        val searchScope: GlobalSearchScope = ProjectScope.getContentScope(project)
        val javaFiles = FileTypeIndex.getFiles(JavaFileType.INSTANCE, searchScope)

        return filterFiles(javaFiles, psiManager, ::controllerFilter)
    }

    private fun getAllDtoFiles(): List<PsiFile> {
        val psiManager = PsiManager.getInstance(project)

        val searchScope: GlobalSearchScope = ProjectScope.getContentScope(project)
        val javaFiles = FileTypeIndex.getFiles(JavaFileType.INSTANCE, searchScope)

        return filterFiles(javaFiles, psiManager, ::dtoFilter)
    }

    private fun getAllServiceFiles(): List<PsiFile> {
        val psiManager = PsiManager.getInstance(project)

        val searchScope: GlobalSearchScope = ProjectScope.getContentScope(project)
        val javaFiles = FileTypeIndex.getFiles(JavaFileType.INSTANCE, searchScope)

        return filterFiles(javaFiles, psiManager, ::serviceFilter)
    }

    private fun filterFiles(
        javaFiles: Collection<VirtualFile>,
        psiManager: PsiManager,
        filter: (PsiClass) -> Boolean,
    ) = javaFiles
        .mapNotNull { virtualFile -> psiManager.findFile(virtualFile) }
        .filter { psiFile ->
            val psiClass = PsiTreeUtil.findChildrenOfType(psiFile, PsiClass::class.java)
                .firstOrNull()
            psiClass != null && filter(psiClass)
        }

    fun addMethodToClass(psiClass: PsiClass, method: String): PsiClass {
        val methodFromText = psiElementFactory.createMethodFromText(method, psiClass)
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

    override fun controllerList(): List<DtClass> {
        return this.controllers.map {
            val className = it.name.substring(0, it.name.length - ".java".length)
            DtClass.fromPsiFile(it) ?: DtClass(className, emptyList())
        }
    }

    override fun serviceList(): List<DtClass> {
        return this.services.map {
            val className = it.name.substring(0, it.name.length - ".java".length)
            DtClass.fromPsiFile(it) ?: DtClass(className, emptyList())
        }
    }

    override fun modelList(): List<DtClass> {
        return this.entities.map {
            val className = it.name.substring(0, it.name.length - ".java".length)
            DtClass.fromPsiFile(it) ?: DtClass(className, emptyList())
        }
    }

    override fun createControllerOrUpdateMethod(targetController: String, code: String, isControllerExist: Boolean) {
        if (!isControllerExist) {
            this.createController(targetController, code)
            return
        }

        val targetControllerFile = controllers.first { it.name == "$targetController.java" }

        ApplicationManager.getApplication().runReadAction {
            val targetControllerClass = PsiTreeUtil.findChildrenOfType(targetControllerFile, PsiClass::class.java)
                .firstOrNull() ?: return@runReadAction // Return from the lambda if the class is not found

            var method = code
            if (code.contains("class $targetController")) {
                method = code.substring(code.indexOf("{") + 1, code.lastIndexOf("}"))
            }

            method = method.trimIndent()

            ApplicationManager.getApplication().invokeLater {
                WriteCommandAction.writeCommandAction(project)
                    .run<RuntimeException> {
                        addMethodToClass(targetControllerClass, method)
                        CodeStyleManager.getInstance(project).reformat(targetControllerFile)
                    }
            }
        }
    }

    override fun createController(endpoint: String, code: String): DtClass? {
        if (controllers.isEmpty()) {
            return DtClass("", emptyList())
        }

        val randomController = firstController()
        val packageStatement = randomController.lookupPackageName()

        if (packageStatement == null) {
            log.warn("No package statement found in file ${randomController.name}")
            return DtClass("", emptyList())
        }

        val templateCode = codeTemplate.controller(endpoint, code, packageStatement.packageName)

        val parentDirectory = randomController.virtualFile?.parent ?: return null
        val fileSystem = randomController.virtualFile?.fileSystem

        createClassByTemplate(parentDirectory, fileSystem, endpoint, templateCode)

        return DtClass(endpoint, emptyList())
    }

    override fun createEntity(code: String): DtClass? {
        return createClassByCode(code, entities)
    }

    override fun createDto(code: String): DtClass? {
        return createClassByCode(code, dto)
    }

    override fun createService(code: String): DtClass? {
        return createClassByCode(code, services)
    }

    private fun createClassByCode(code: String, psiFiles: List<PsiFile>): DtClass? {
        val firstFile = psiFiles.first()
        val packageName = if (psiFiles.isNotEmpty()) {
            firstFile.lookupPackageName()?.packageName
        } else {
            packageCloseToController("service")
        }

        val newCode = "package $packageName;\n\n$code"

        if (packageName == null) {
            log.warn("No package statement found in file ${firstFile.name}")
            return DtClass("", emptyList())
        }

        return createClass(newCode, packageName)
    }

    override fun createClass(code: String, packageName: String?): DtClass? {
        // controller parent will be ${package}.controller, ${package}.controller parent will be ${package}
        var parentDirectory = firstController().virtualFile?.parent?.parent ?: return null
        val fileSystem = firstController().virtualFile?.fileSystem
        ApplicationManager.getApplication().invokeLater {
            runWriteAction {
                val newClass = psiElementFactory.createClassFromText(code, null)

                val regex = Regex("public\\s+class\\s+(\\w+)")
                val matchResult = regex.find(code)

                val className = if (matchResult?.groupValues?.get(1) != null) {
                    matchResult.groupValues[1]
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
                    log.warn("File $targetClass already exists")
                    return@runWriteAction
                }

                val virtualFile = parentDirectory.createChildData(fileSystem, targetClass)
                VfsUtil.saveText(virtualFile, code)

                log.warn("Created file ${virtualFile.path}")
                parentDirectory.refresh(false, true)
            }
        }

        return null
    }

    private fun packageCloseToController(subpackage: String): String? {
        val firstControllerPkg = firstController().lookupPackageName()?.packageName
        if (firstControllerPkg != null) {
            val lastDotIndex = firstControllerPkg.lastIndexOf(".")
            if (lastDotIndex != -1) {
                return firstControllerPkg.substring(0, lastDotIndex) + "." + subpackage
            }
        }

        return firstControllerPkg
    }

    private fun firstController() = controllers.first()

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

                log.warn("Created file ${virtualFile.path}")
                parentDirectory.refresh(false, true)
            }
        }
    }

    companion object {
        private val log: Logger = logger<JavaSpringBaseCrud>()
    }
}

fun PsiFile.lookupPackageName(): PsiPackageStatement? {
    val packageStatement = runReadAction {
        PsiTreeUtil.findChildrenOfType(this, PsiPackageStatement::class.java)
            .firstOrNull() ?: return@runReadAction null
    }
    return packageStatement
}
