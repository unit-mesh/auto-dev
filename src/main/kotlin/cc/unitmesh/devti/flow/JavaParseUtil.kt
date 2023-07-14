package cc.unitmesh.devti.flow

object JavaParseUtil {
    // split it to 3 parts:
    fun splitClass(code: String): List<String> {
        val classPattern = Regex("(public\\s+class|public\\s+interface).*?\\{.*?}", RegexOption.DOT_MATCHES_ALL)
        val matches = classPattern.findAll(code)
        return matches.map { it.value }.toList()
    }
}