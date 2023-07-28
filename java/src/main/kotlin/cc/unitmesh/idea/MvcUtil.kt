package cc.unitmesh.idea

object MvcUtil {
    fun langSuffix(lang: String): String = when (lang.lowercase()) {
        "java" -> "java"
        "kotlin" -> "kt"
        else -> "java"
    }

    fun isController(fileName: String, lang: String = "java"): Boolean {
        return fileName.endsWith("Controller." + langSuffix(lang))
    }

    fun isService(fileName: String, lang: String = "java") =
        fileName.endsWith("Service." + langSuffix(lang)) || fileName.endsWith("ServiceImpl." + langSuffix(lang))
}