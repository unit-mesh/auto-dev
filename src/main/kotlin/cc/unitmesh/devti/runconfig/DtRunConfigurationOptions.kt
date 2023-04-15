package cc.unitmesh.devti.runconfig

import com.intellij.execution.configurations.RunConfigurationOptions
import com.intellij.openapi.components.StoredProperty


class DtRunConfigurationOptions : RunConfigurationOptions() {
    val githubToken: StoredProperty<String?> = string("").provideDelegate(this, "githubToken")
    val openAiApiKey: StoredProperty<String?> = string("").provideDelegate(this, "openAiApiKey")
    val openAiEngine: StoredProperty<String?> = string("").provideDelegate(this, "openAiEngine")
    val openAiMaxTokens: StoredProperty<Int> = property(4096).provideDelegate(this, "openAiMaxTokens")
}
