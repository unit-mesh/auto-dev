package cc.unitmesh.devti.flow

import cc.unitmesh.devti.analysis.DtClass
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile

// TODO: remove this interface
interface SpringBaseCrud {
    fun controllerList(): List<DtClass>
    fun entityList(): List<DtClass>
    fun serviceList(): List<DtClass>

    /**
     * return all entity class + dto class
     */
    fun modelList(): List<DtClass>
    fun getAllControllerFiles(): List<PsiFile>
    fun getAllEntityFiles(): List<PsiFile>
    fun getAllDtoFiles(): List<PsiFile>
    fun getAllServiceFiles(): List<PsiFile>
    fun getAllRepositoryFiles(): List<PsiFile>


    fun createControllerOrUpdateMethod(targetController: String, code: String, isControllerExist: Boolean)
    fun createController(endpoint: String, code: String): DtClass?
    fun createEntity(code: String): DtClass?
    fun createService(code: String): DtClass?
    fun createDto(code: String): DtClass?
    fun createRepository(code: String): DtClass?
    fun createClass(code: String, packageName: String?): DtClass?

    fun dtoFilter(clazz: PsiClass): Boolean {
        val className = clazz.name?.lowercase()

        return (className?.endsWith("dto") == true ||
                className?.endsWith("request") == true
                || className?.endsWith("response") == true)
    }

    fun controllerFilter(clazz: PsiClass): Boolean = clazz.annotations
        .map { it.qualifiedName }.any {
            it == "org.springframework.stereotype.Controller" ||
                    it == "org.springframework.web.bind.annotation.RestController"
        }

    fun serviceFilter(clazz: PsiClass): Boolean = clazz.annotations
        .map { it.qualifiedName }.any {
            it == "org.springframework.stereotype.Service"
        }

    fun repositoryFilter(clazz: PsiClass): Boolean = clazz.annotations
        .map { it.qualifiedName }.any {
            it == "org.springframework.stereotype.Repository"
        }

    fun entityFilter(clazz: PsiClass): Boolean = clazz.annotations
        .map { it.qualifiedName }.any {
            it == "javax.persistence.Entity"
        }

    fun isController(code: String): Boolean = SpringLayerCharacteristic.checkLayer(code, "controller")
    fun isService(code: String): Boolean = SpringLayerCharacteristic.checkLayer(code, "service")
    fun isEntity(code: String): Boolean = SpringLayerCharacteristic.checkLayer(code, "entity")
    fun isDto(code: String): Boolean = SpringLayerCharacteristic.checkLayer(code, "dto")
    fun isRepository(code: String): Boolean = SpringLayerCharacteristic.checkLayer(code, "repository")
}
