package cc.unitmesh.devti.flow

import cc.unitmesh.devti.analysis.DtClass
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile

// TODO: remove this interface
interface SpringBaseCrud {
    @Deprecated("use getAllControllerFiles instead")
    fun controllerList(): List<DtClass>

    @Deprecated("use getAllServiceFiles instead")
    fun serviceList(): List<DtClass>

    fun modelList(): List<DtClass> {
        val files = this.getAllEntityFiles() + this.getAllDtoFiles()
        return files.map(DtClass.Companion::fromJavaFile)
    }

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

    fun dtoFilter(clazz: PsiClass): Boolean = SpringLayerCharacteristic.checkLayer(clazz, "dto")
    fun entityFilter(clazz: PsiClass): Boolean = SpringLayerCharacteristic.checkLayer(clazz, "entity")
    fun controllerFilter(clazz: PsiClass): Boolean = SpringLayerCharacteristic.checkLayer(clazz, "controller")
    fun serviceFilter(clazz: PsiClass): Boolean = SpringLayerCharacteristic.checkLayer(clazz, "service")
    fun repositoryFilter(clazz: PsiClass): Boolean = SpringLayerCharacteristic.checkLayer(clazz, "repository")

    fun isDto(code: String): Boolean = SpringLayerCharacteristic.checkLayer(code, "dto")
    fun isEntity(code: String): Boolean = SpringLayerCharacteristic.checkLayer(code, "entity")
    fun isController(code: String): Boolean = SpringLayerCharacteristic.checkLayer(code, "controller")
    fun isService(code: String): Boolean = SpringLayerCharacteristic.checkLayer(code, "service")
    fun isRepository(code: String): Boolean = SpringLayerCharacteristic.checkLayer(code, "repository")
}
