package cc.unitmesh.devti.provider.http

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project

interface HttpClientProvider {

    fun execute(project: Project, text: String)

    companion object {
        private val EP_NAME: ExtensionPointName<HttpClientProvider> =
            ExtensionPointName("cc.unitmesh.httpClientExecutor")

        fun all(): List<HttpClientProvider> {
            return EP_NAME.extensionList
        }
    }
}