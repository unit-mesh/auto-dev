package cc.unitmesh.ide.javascript.flow.model

import cc.unitmesh.ide.javascript.flow.ReactAutoPage

data class AutoPageContext(
    val requirement: String,
    var pages: List<String>,
    val pageNames: List<String>,
    var components: List<String>,
    val componentNames: List<String>,
    val routes: List<String>,
) {
    companion object {
        fun build(reactAutoPage: ReactAutoPage): AutoPageContext {
            return AutoPageContext(
                requirement = reactAutoPage.userTask,
                pages = reactAutoPage.getPages().map { it.format() },
                pageNames = reactAutoPage.getPages().map { it.name },
                components = reactAutoPage.getComponents().map { it.format() },
                componentNames = reactAutoPage.getComponents().map { it.name },
                routes = reactAutoPage.getRoutes(),
            )
        }
    }
}