package cc.unitmesh.devti.prompting.model

data class TestStack(
    val controller: MutableMap<String, Boolean> = mutableMapOf(),
    val service: MutableMap<String, Boolean> = mutableMapOf()
)