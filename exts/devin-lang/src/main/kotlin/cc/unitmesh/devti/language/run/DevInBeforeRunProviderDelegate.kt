package cc.unitmesh.devti.language.run

import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.impl.RunConfigurationBeforeRunProviderDelegate
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.util.Key

class DevInBeforeRunProviderDelegate : RunConfigurationBeforeRunProviderDelegate {
    val DEVIN_BEFORE_KEY_PREFIX: String = "DevIn.BeforeRunTask"
    val KEY_MAP: MutableMap<String, Key<Boolean>> = HashMap()

    override fun beforeRun(environment: ExecutionEnvironment) {
        val settings = environment.runnerAndConfigurationSettings ?: return
        val configuration = settings.configuration
        if (configuration is AutoDevConfiguration) {
            val userDataKey = getRunBeforeUserDataKey(configuration)
            configuration.project.putUserData(userDataKey, true)
        }
    }

    fun getRunBeforeUserDataKey(runConfiguration: RunConfiguration): Key<Boolean> {
        return KEY_MAP.computeIfAbsent(
            runConfiguration.name
        ) { key: String ->
            Key.create(
                DEVIN_BEFORE_KEY_PREFIX + "_" + key
            )
        }
    }

}
