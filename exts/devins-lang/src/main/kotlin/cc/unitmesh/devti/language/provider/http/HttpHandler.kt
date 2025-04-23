package cc.unitmesh.devti.language.provider.http

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project

interface HttpHandler {
    fun isApplicable(type: HttpHandlerType): Boolean

    fun execute(project: Project, content: String, variablesName: Array<String>, variableTable: MutableMap<String, Any?>) : String?

    companion object {
        private val EP_NAME: ExtensionPointName<HttpHandler> =
            ExtensionPointName("cc.unitmesh.shireHttpHandler")

        fun provide(type: HttpHandlerType): HttpHandler? {
            return EP_NAME.extensionList.find { it.isApplicable(type) }
        }
    }
}

enum class HttpHandlerType(val id: String) {
    CURL("cURL"),
}