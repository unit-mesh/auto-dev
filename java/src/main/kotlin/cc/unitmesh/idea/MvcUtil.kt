package cc.unitmesh.idea

object MvcUtil {
    fun isController(fileName: String, lang: String): Boolean {
        return fileName.endsWith("Controller.$lang")
    }

    fun isService(fileName: String, lang: String) =
        fileName.endsWith("Service.$lang") || fileName.endsWith("ServiceImpl.$lang")
}