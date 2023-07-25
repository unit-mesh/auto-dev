package cc.unitmesh.devti.prompting.code

data class TestStack(
    val controller: MutableMap<String, Boolean> = mutableMapOf(),
    val service: MutableMap<String, Boolean> = mutableMapOf(),
    val deps: MutableMap<String, String> = mutableMapOf(),
    val devDeps: MutableMap<String, String> = mutableMapOf()
) {
    fun controllerString(): String {
        return controller.keys.joinToString(", ")
    }

    fun serviceString(): String {
        return service.keys.joinToString(", ")
    }
}