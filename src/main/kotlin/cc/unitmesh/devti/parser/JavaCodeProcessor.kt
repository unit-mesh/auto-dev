package cc.unitmesh.devti.parser

class JavaCodeProcessor {
    companion object {
        fun findUsageCode(code: String, calleeName: String): List<String> {
            // a serviceName should be uppercase, we need to convert first letter to lowercase
            val usedServiceName = calleeName.replaceFirstChar { it.lowercase() }

            //  use regex to match " xxxServer.xxxMethod(xxx);
            val usedMethodCode = Regex("""\s+($usedServiceName\..*\(.*\))[^\n]""")
                .findAll(code)
                .map { it.value.trim() }
                .toList()

            return usedMethodCode
        }
    }
}