package cc.unitmesh.devti.analysis

interface AutoCurdData {
    fun controllerList(): List<DtFile>
    fun serviceList(): List<DtFile>
    fun modelList(): List<DtFile>
}
