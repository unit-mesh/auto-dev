package cc.unitmesh.devti.custom.variable

import cc.unitmesh.devti.custom.action.CustomPromptConfig
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service

@Service(Service.Level.APP)
class SpecResolverService {
    private val specs = CustomPromptConfig.load().spec

    fun resolvers(): List<SpecVariableResolver> = specs.map { (key, value) ->
        SpecVariableResolver("SPEC_$key", value)
    }

    companion object {
        fun getInstance(): SpecResolverService =
            ApplicationManager.getApplication().getService(SpecResolverService::class.java)
    }
}