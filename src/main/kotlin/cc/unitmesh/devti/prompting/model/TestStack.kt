package cc.unitmesh.devti.prompting.model

data class TestStack(
    val controller: MutableMap<String, Boolean> = mutableMapOf(),
    val service: MutableMap<String, Boolean> = mutableMapOf()
) {
    fun controllerString(): String {
        return controller.keys.joinToString(", ")
    }

    fun serviceString(): String {
        return service.keys.joinToString(", ")
    }
}