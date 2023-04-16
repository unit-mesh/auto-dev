package cc.unitmesh.devti.analysis

interface CrudProcessor {
    fun controllerList(): List<DtFile>
    fun serviceList(): List<DtFile>
    fun modelList(): List<DtFile>
}
