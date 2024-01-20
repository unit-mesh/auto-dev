package cc.unitmesh.idea.spring

import cc.unitmesh.devti.context.model.DtClass
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile

// TODO: remove this interface
interface SpringBaseCrud {
    fun getAllDtoFiles(): List<PsiFile>
    fun getAllEntityFiles(): List<PsiFile>
    fun getAllControllerFiles(): List<PsiFile>
    fun getAllServiceFiles(): List<PsiFile>
    fun getAllRepositoryFiles(): List<PsiFile>

    fun createControllerOrUpdateMethod(targetController: String, code: String, isControllerExist: Boolean)
    fun createDto(code: String): DtClass?
    fun createEntity(code: String): DtClass?
    fun createController(endpoint: String, code: String): DtClass?
    fun createService(code: String): DtClass?
    fun createRepository(code: String): DtClass?
    fun createClass(code: String, packageName: String?): DtClass?

    fun dtoFilter(clazz: PsiClass): Boolean = SpringLayerCharacteristic.check(clazz, "dto")
    fun entityFilter(clazz: PsiClass): Boolean = SpringLayerCharacteristic.check(clazz, "entity")
    fun controllerFilter(clazz: PsiClass): Boolean = SpringLayerCharacteristic.check(clazz, "controller")
    fun serviceFilter(clazz: PsiClass): Boolean = SpringLayerCharacteristic.check(clazz, "service")
    fun repositoryFilter(clazz: PsiClass): Boolean = SpringLayerCharacteristic.check(clazz, "repository")

    fun isDto(code: String): Boolean = SpringLayerCharacteristic.check(code, "dto")
    fun isEntity(code: String): Boolean = SpringLayerCharacteristic.check(code, "entity")
    fun isController(code: String): Boolean = SpringLayerCharacteristic.check(code, "controller")
    fun isService(code: String): Boolean = SpringLayerCharacteristic.check(code, "service")
    fun isRepository(code: String): Boolean = SpringLayerCharacteristic.check(code, "repository")
    fun updateMethod(targetFile: PsiFile, targetClass: String, code: String)
}
