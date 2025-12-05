package cc.unitmesh.devti.settings.coder

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.fullWidthCell
import cc.unitmesh.devti.settings.locale.LanguageChangedCallback.i18nLabel
import cc.unitmesh.devti.settings.locale.LanguageChangedCallback.tips
import com.intellij.openapi.components.service
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.HyperlinkLabel
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.toMutableProperty
import org.jetbrains.builtInWebServer.BuiltInServerOptions
import java.net.InetAddress
import javax.swing.JCheckBox
import javax.swing.JTextField

class AutoDevCoderConfigurable(private val project: Project) : BoundConfigurable(AutoDevBundle.message("settings.autodev.coder")) {
    private val recordingInLocalCheckBox = JCheckBox()
    private val disableAdvanceContextCheckBox = JCheckBox().apply {
        tips("settings.autodev.coder.disableAdvanceContext.tips", this)
    }
    private val inEditorCompletionCheckBox = JCheckBox()
    private val noChatHistoryCheckBox = JCheckBox()
    private val enableMcpServerCheckBox = JCheckBox()
    private val enableObserver = JCheckBox()
    private val teamPromptsField = JTextField()
    private val trimCodeBeforeSend = JCheckBox()
    private val enableAutoRepairDiff = JCheckBox()
    private val enableAutoRunTerminal = JCheckBox()
    private val enableAutoLintCode = JCheckBox()
    private val enableRenderWebview = JCheckBox()
    private val enableAutoScrollInSketch = JCheckBox()


    val settings = project.service<AutoDevCoderSettingService>()
    val state = settings.state.copy()

    override fun createPanel(): DialogPanel = panel {
        row(i18nLabel("settings.autodev.coder.recordingInLocal")) {
            fullWidthCell(recordingInLocalCheckBox)
                .bind(
                    componentGet = { it.isSelected },
                    componentSet = { component, value -> component.isSelected = value },
                    prop = state::recordingInLocal.toMutableProperty()
                )
        }

        row(i18nLabel("settings.autodev.coder.disableAdvanceContext")) {
            fullWidthCell(disableAdvanceContextCheckBox)
                .bind(
                    componentGet = { it.isSelected },
                    componentSet = { component, value -> component.isSelected = value },
                    prop = state::disableAdvanceContext.toMutableProperty()
                )
        }

        row(i18nLabel("settings.autodev.coder.noChatHistory")) {
            fullWidthCell(noChatHistoryCheckBox)
                .bind(
                    componentGet = { it.isSelected },
                    componentSet = { component, value -> component.isSelected = value },
                    prop = state::noChatHistory.toMutableProperty()
                )
        }

        row(i18nLabel("settings.autodev.coder.enableExportAsMcpServer")) {
            fullWidthCell(enableMcpServerCheckBox)
                .bind(
                    componentGet = { it.isSelected },
                    componentSet = { component, value -> component.isSelected = value },
                    prop = state::enableExportAsMcpServer.toMutableProperty()
                )

            comment("<font color='red'>* ${AutoDevBundle.message("settings.autodev.coder.requires.restart")}</font>")

            val port = BuiltInServerOptions.getInstance().builtInServerPort
            val hostname = InetAddress.getLoopbackAddress().hostAddress
            val serverUrl = "http://$hostname:$port/api/mcp/list_tools"
            val portLabel = HyperlinkLabel(serverUrl)
            portLabel.setHyperlinkTarget(serverUrl)
            cell(portLabel)
        }

        row(i18nLabel("settings.autodev.coder.enableObserver")) {
            fullWidthCell(enableObserver)
                .bind(
                    componentGet = { it.isSelected },
                    componentSet = { component, value -> component.isSelected = value },
                    prop = state::enableObserver.toMutableProperty()
                )
        }

        row(i18nLabel("settings.autodev.coder.trimCodeBeforeSend")) {
            fullWidthCell(trimCodeBeforeSend)
                .bind(
                    componentGet = { it.isSelected },
                    componentSet = { component, value -> component.isSelected = value },
                    prop = state::trimCodeBeforeSend.toMutableProperty()
                )
        }

        row(i18nLabel("settings.autodev.coder.inEditorCompletion")) {
            fullWidthCell(inEditorCompletionCheckBox)
                .bind(
                    componentGet = { it.isSelected },
                    componentSet = { component, value -> component.isSelected = value },
                    prop = state::inEditorCompletion.toMutableProperty()
                )
        }

        row(i18nLabel("settings.autodev.coder.enableRenameSuggestion")) {
            fullWidthCell(JCheckBox())
                .bind(
                    componentGet = { it.isSelected },
                    componentSet = { component, value -> component.isSelected = value },
                    prop = state::enableRenameSuggestion.toMutableProperty()
                )
        }

        row(i18nLabel("settings.autodev.coder.enableAutoRepairDiff")) {
            fullWidthCell(enableAutoRepairDiff)
                .bind(
                    componentGet = { it.isSelected },
                    componentSet = { component, value -> component.isSelected = value },
                    prop = state::enableAutoRepairDiff.toMutableProperty()
                )
        }

        row(i18nLabel("settings.autodev.coder.enableAutoRunTerminal")) {
            fullWidthCell(enableAutoRunTerminal)
                .bind(
                    componentGet = { it.isSelected },
                    componentSet = { component, value -> component.isSelected = value },
                    prop = state::enableAutoRunTerminal.toMutableProperty()
                )
        }

        row(i18nLabel("settings.autodev.coder.enableAutoLintCode")) {
            fullWidthCell(enableAutoLintCode)
                .bind(
                    componentGet = { it.isSelected },
                    componentSet = { component, value -> component.isSelected = value },
                    prop = state::enableAutoLintCode.toMutableProperty()
                )
        }

        row(i18nLabel("settings.autodev.coder.enableRenderWebview")) {
            fullWidthCell(enableRenderWebview)
                .bind(
                    componentGet = { it.isSelected },
                    componentSet = { component, value -> component.isSelected = value },
                    prop = state::enableRenderWebview.toMutableProperty()
                )
        }

        row(i18nLabel("settings.autodev.coder.enableAutoScrollInSketch")) {
            fullWidthCell(enableAutoScrollInSketch)
                .bind(
                    componentGet = { it.isSelected },
                    componentSet = { component, value -> component.isSelected = value },
                    prop = state::enableAutoScrollInSketch.toMutableProperty()
                )
        }

        row(i18nLabel("settings.autodev.coder.enableDiffViewer")) {
            fullWidthCell(JCheckBox())
                .bind(
                    componentGet = { it.isSelected },
                    componentSet = { component, value -> component.isSelected = value },
                    prop = state::enableDiffViewer.toMutableProperty()
                )
        }

        row(i18nLabel("settings.external.team.prompts.path")) {
            fullWidthCell(teamPromptsField)
                .bind(
                    componentGet = { it.text },
                    componentSet = { component, value -> component.text = value },
                    prop = state::teamPromptsDir.toMutableProperty()
                )
        }

        onApply {
            settings.modify {
                it.recordingInLocal = state.recordingInLocal
                it.disableAdvanceContext = state.disableAdvanceContext
                it.inEditorCompletion = state.inEditorCompletion
                it.noChatHistory = state.noChatHistory
                it.enableRenameSuggestion = state.enableRenameSuggestion
                it.trimCodeBeforeSend = state.trimCodeBeforeSend
                it.teamPromptsDir = state.teamPromptsDir
                it.enableExportAsMcpServer = state.enableExportAsMcpServer
                it.enableObserver = state.enableObserver
                it.enableAutoRepairDiff = state.enableAutoRepairDiff
                it.enableAutoRunTerminal = state.enableAutoRunTerminal
                it.enableAutoLintCode = state.enableAutoLintCode
                it.enableRenderWebview = state.enableRenderWebview
                it.enableAutoScrollInSketch = state.enableAutoScrollInSketch
                it.enableDiffViewer = state.enableDiffViewer
            }
        }
    }
}
