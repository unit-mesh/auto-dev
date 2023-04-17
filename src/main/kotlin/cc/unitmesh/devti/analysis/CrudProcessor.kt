package cc.unitmesh.devti.analysis

interface CrudProcessor {
    fun controllerList(): List<DtClass>
    fun serviceList(): List<DtClass>
    fun modelList(): List<DtClass>
    fun updateMethod(targetController: String, code: String)
    fun createController(endpoint: String): DtClass
}
