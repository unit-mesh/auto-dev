package cc.unitmesh.devti.analysis

interface CrudProcessor {
    fun controllerList(): List<DtClass>
    fun serviceList(): List<DtClass>
    fun modelList(): List<DtClass>
    fun createControllerOrUpdateMethod(targetController: String, code: String, isControllerExist: Boolean)
    fun createController(endpoint: String, code: String): DtClass?
    fun isService(code: String): Boolean
    fun createService(serviceName: String, code: String): DtClass?
}
