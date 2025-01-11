package cc.unitmesh.devti.settings.devops

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.settings.custom.TeamPromptsProjectSettingsService
import com.intellij.openapi.components.*
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurableProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComboBox
import javax.swing.JPasswordField
import javax.swing.JTextField

class AutoDevDevOpsConfigurableProvider(private val project: Project) : ConfigurableProvider() {
    override fun createConfigurable(): Configurable {
        return DevOpsConfigurable(project)
    }
}

val GIT_TYPE = arrayOf("Github" , "Gitlab")
val DEFAULT_GIT_TYPE = GIT_TYPE[0]

class DevOpsConfigurable(project: Project) : BoundConfigurable(AutoDevBundle.message("settings.autodev.devops")) {
    private val settings = AutoDevDevOpsSettingService.getInstance(project)

    private lateinit var gitTypeComboBox: JComboBox<String>
    private lateinit var githubTokenField: JPasswordField  
    private lateinit var gitlabUrlField: JTextField
    private lateinit var gitlabTokenField: JPasswordField

    override fun createPanel(): DialogPanel {
        return panel {
            group("Git Configuration") {
                row("Git Type:") {
                    gitTypeComboBox = comboBox(GIT_TYPE.toList()).component
                }
                row("GitHub Token:") {
                    githubTokenField = passwordField().component
                }
                row("GitLab URL:") {
                    gitlabUrlField = textField().component
                }
                row("GitLab Token:") {
                    gitlabTokenField = passwordField().component
                }
            }
        }
    }

    override fun apply() {
        settings.modify { state ->
            state.githubToken = githubTokenField.password.toString()
            state.gitlabUrl = gitlabUrlField.text
            state.gitlabToken = gitlabTokenField.password.toString()
        }
    }

    override fun reset() {
        githubTokenField.text = settings.state.githubToken
        gitlabUrlField.text = settings.state.gitlabUrl
        gitlabTokenField.text = settings.state.gitlabToken
    }

    override fun isModified(): Boolean {
        return settings.state.githubToken != githubTokenField.password.toString() ||
                settings.state.gitlabUrl != gitlabUrlField.text ||
                settings.state.gitlabToken != gitlabTokenField.password.toString()
    }
}

val Project.devopsPromptsSettings: AutoDevDevOpsSettingService get() = service<AutoDevDevOpsSettingService>()

@Service(Service.Level.PROJECT)
@State(name = "AutoDevDevOpsSettings", storages = [Storage("autodev-devops.xml")])
class AutoDevDevOpsSettingService(
    val project: Project,
) : SimplePersistentStateComponent<AutoDevDevOpsSettingService.AutoDevCoderSettings>(AutoDevCoderSettings()) {
    fun modify(action: (AutoDevCoderSettings) -> Unit) {
        action(state)
    }

    companion object {
        @JvmStatic
        fun getInstance(project: Project): AutoDevDevOpsSettingService {
            return project.getService(AutoDevDevOpsSettingService::class.java)
        }
    }

    abstract class AdProjectSettingsBase<T : AdProjectSettingsBase<T>> : BaseState() {
        abstract fun copy(): T
    }

    class AutoDevCoderSettings : AdProjectSettingsBase<AutoDevCoderSettings>() {
        var recordingInLocal by property(false)
        var gitType = DEFAULT_GIT_TYPE
        var githubToken = ""
        var gitlabToken = ""
        var gitlabUrl = ""

        override fun copy(): AutoDevCoderSettings {
            val state = AutoDevCoderSettings()
            state.copyFrom(this)
            return state
        }
    }
}