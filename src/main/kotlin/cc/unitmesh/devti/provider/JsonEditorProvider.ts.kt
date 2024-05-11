package cc.unitmesh.devti.provider

import cc.unitmesh.devti.provider.devins.DevInsSymbolProvider
import com.intellij.openapi.extensions.ExtensionPointName
import java.awt.Component

interface JsonTextProvider {
    fun createComponent() : Component;

    companion object {
        private val EP_NAME: ExtensionPointName<DevInsSymbolProvider> =
            ExtensionPointName("cc.unitmesh.jsonTextProvider")

        fun all(): List<DevInsSymbolProvider> {
            return EP_NAME.extensionList
        }
    }
}