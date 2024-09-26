package cc.unitmesh.devti.custom.variable

import cc.unitmesh.devti.custom.action.CustomPromptConfig
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service

/**
 * For resolve tech spec variables which maybe define by user.
 */
@Service(Service.Level.APP)
class SpecResolverService {
    fun resolvers(): List<SpecVariableResolver> = CustomPromptConfig.load().spec.map { (key, value) ->
        SpecVariableResolver("SPEC_$key", value)
    }

    companion object {
        fun getInstance(): SpecResolverService =
            ApplicationManager.getApplication().getService(SpecResolverService::class.java)
    }
}