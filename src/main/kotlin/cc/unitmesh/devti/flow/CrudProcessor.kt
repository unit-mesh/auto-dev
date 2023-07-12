package cc.unitmesh.devti.flow

import cc.unitmesh.devti.analysis.DtClass

interface CrudProcessor {
    fun controllerList(): List<DtClass>
    fun serviceList(): List<DtClass>
    fun modelList(): List<DtClass>
    fun createControllerOrUpdateMethod(targetController: String, code: String, isControllerExist: Boolean)
    fun createController(endpoint: String, code: String): DtClass?
    fun isService(code: String): Boolean
    fun isDto(code: String): Boolean
    fun createService(code: String): DtClass?
    fun createDto(code: String): DtClass?
    fun createClass(code: String, packageName: String?): DtClass?
    fun isController(code: String): Boolean
}
