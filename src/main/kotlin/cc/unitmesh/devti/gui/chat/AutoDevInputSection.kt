package cc.unitmesh.devti.gui.chat

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.AutoDevIcons
import cc.unitmesh.devti.counit.configurable.customAgentSetting
import cc.unitmesh.devti.counit.model.CustomAgentConfig
import cc.unitmesh.devti.counit.model.CustomAgentState
import cc.unitmesh.devti.custom.compile.CustomVariable
import cc.unitmesh.devti.llms.tokenizer.Tokenizer
import cc.unitmesh.devti.llms.tokenizer.TokenizerImpl
import cc.unitmesh.devti.settings.AutoDevSettingsState
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.ComponentValidator
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.JBPopupListener
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.openapi.wm.impl.InternalDecorator
import com.intellij.temporary.gui.block.AutoDevCoolBorder
import com.intellij.ui.JBColor
import com.intellij.ui.MutableCollectionComboBoxModel
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBLabel
import com.intellij.ui.content.ContentManager
import com.intellij.util.EventDispatcher
import com.intellij.util.ui.JBEmptyBorder
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.components.BorderLayoutPanel
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.awt.Point
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.function.Supplier
import javax.swing.Box
import javax.swing.JComponent
import kotlin.math.max
import kotlin.math.min

/**
 *
 */
class AutoDevInputSection(private val project: Project, val disposable: Disposable?) : BorderLayoutPanel() {
    private val input: AutoDevInput
    private val documentListener: DocumentListener
    private val buttonPresentation: Presentation
    private val button: ActionButton

    private val defaultRag: CustomAgentConfig = CustomAgentConfig("<Select Custom Agent>", "Normal")
    private var customRag: ComboBox<CustomAgentConfig> = ComboBox(MutableCollectionComboBoxModel(listOf()))

    private val logger = logger<AutoDevInputSection>()

    private var popup: JBPopup? = null

    val editorListeners = EventDispatcher.create(AutoDevInputListener::class.java)
    private var tokenizer: Tokenizer? = null
    var text: String
        get() {
            return input.text
        }
        set(text) {
            input.recreateDocument()
            input.text = text
        }

    init {
        val presentation = Presentation(AutoDevBundle.message("chat.panel.send"))
        presentation.setIcon(AutoDevIcons.Send)
        buttonPresentation = presentation
        button = ActionButton(
            DumbAwareAction.create {
                object : DumbAwareAction("") {
                    override fun actionPerformed(e: AnActionEvent) {
                        editorListeners.multicaster.onSubmit(this@AutoDevInputSection, AutoDevInputTrigger.Button)
                    }
                }.actionPerformed(it)
            },
            buttonPresentation,
            "",
            Dimension(20, 20)
        )

        input = AutoDevInput(project, listOf(), disposable, this)

        documentListener = object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                val i = input.preferredSize?.height
                if (i != input.height) {
                    revalidate()
                }

                // check new input == $
                if (event.newFragment.contentEquals("$") || event.newFragment.contentEquals("¥")) {
                    if (popup == null) {
                        popup = createPopup()
                    }

                    if (popup?.isVisible == true) {
                        popup?.cancel()
                    }

                    if (popup?.isDisposed == true) {
                        popup = createPopup()
                        showPopupAbove(popup!!, this@AutoDevInputSection)
                    } else {
                        showPopupAbove(popup!!, this@AutoDevInputSection)
                    }
                }
            }
        }

        input.addDocumentListener(documentListener)
        input.recreateDocument()

        input.border = JBEmptyBorder(4)

        addToCenter(input)
        val layoutPanel = BorderLayoutPanel()
        val horizontalGlue = Box.createHorizontalGlue()
        horizontalGlue.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                val ideFocusManager = IdeFocusManager.getInstance(project)
                ideFocusManager.requestFocus(input, true)
            }
        })
        layoutPanel.background = JBColor(
            JBColor(15461616, 14672357),
            JBColor(3684930, 3750720)
        )
        layoutPanel.setOpaque(false)

        if (project.customAgentSetting.enableCustomRag) {
            customRag = ComboBox(MutableCollectionComboBoxModel(loadRagApps()))
            customRag.setRenderer(SimpleListCellRenderer.create { label: JBLabel, value: CustomAgentConfig?, _: Int ->
                if (value != null) {
                    label.text = value.name
                }
            })
            customRag.selectedItem = defaultRag

            layoutPanel.addToLeft(customRag)
        }

        layoutPanel.addToCenter(horizontalGlue)
        layoutPanel.addToRight(button)
        addToBottom(layoutPanel)

        ComponentValidator(disposable!!).withValidator(Supplier<ValidationInfo?> {
            val validationInfo: ValidationInfo? = this.getInputValidationInfo()
            button.setEnabled(validationInfo == null)
            return@Supplier validationInfo
        }).installOn((this as JComponent)).revalidate()

        addListener(object : AutoDevInputListener {
            override fun editorAdded(editor: EditorEx) {
                this@AutoDevInputSection.initEditor()
            }
        })

        tokenizer = TokenizerImpl.INSTANCE
    }

    private fun createPopup(): JBPopup {
        val list: AutoDevVariableList = AutoDevVariableList.from(CustomVariable.all()) { item ->
            input.text += item.customVariable.variable
            this.popup?.cancel()
        }

        list.selectedIndex = 0

        list.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent?) {
                if (!hasPopup()) return

                when (e?.keyCode) {
                    KeyEvent.VK_ENTER -> {
                        e.consume()
                        val selectedItem = list.getSelectedValue()
                        if (selectedItem != null) {
                            text += "${selectedItem.customVariable.variable} "
                        }
                        this@AutoDevInputSection.popup?.cancel()
                        this@AutoDevInputSection.requestFocus()
                    }

                    KeyEvent.VK_DOWN -> {
                        val selectedIndex = list.selectedIndex
                        val itemsCount = list.getItemsCount()
                        if (selectedIndex < itemsCount - 1) {
                            list.setSelectedIndex(selectedIndex + 1)
                        } else {
                            list.setSelectedIndex(0)
                        }
                    }

                    KeyEvent.VK_UP -> {
                        val selectedIndex = list.selectedIndex
                        if (selectedIndex > 0) {
                            list.setSelectedIndex(selectedIndex - 1)
                        } else {
                            list.setSelectedIndex(list.getItemsCount() - 1)
                        }
                    }

                    // Esc
                    KeyEvent.VK_ESCAPE -> {
                        this@AutoDevInputSection.requestFocus()
                    }
                }
            }
        })

        val popups = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(list, null)
            .setFocusable(true)
            .setRequestFocus(true)
            .setMinSize(Dimension(this@AutoDevInputSection.width, 0))
            .createPopup()

        return popups
    }

    fun hasPopup(): Boolean {
        return popup?.isVisible == true && popup?.isDisposed == false
    }

    private fun loadRagApps(): List<CustomAgentConfig> {
        val ragsJsonConfig = project.customAgentSetting.ragsJsonConfig
        if (ragsJsonConfig.isEmpty()) return listOf(defaultRag)

        val rags = try {
            Json.decodeFromString<List<CustomAgentConfig>>(ragsJsonConfig)
        } catch (e: Exception) {
            logger.warn("Failed to parse custom rag apps", e)
            listOf()
        }

        return listOf(defaultRag) + rags
    }

    fun initEditor() {
        val editorEx = this.input.editor as? EditorEx ?: return

        setBorder(AutoDevCoolBorder(editorEx, this))
        UIUtil.setOpaqueRecursively(this, false)
        this.revalidate()
    }


    override fun getPreferredSize(): Dimension {
        val result = super.getPreferredSize()
        result.height = max(min(result.height, maxHeight), minimumSize.height)
        return result
    }

    fun setContent(trimMargin: String) {
        val focusManager = IdeFocusManager.getInstance(project)
        focusManager.requestFocus(input, true)
        this.input.recreateDocument()
        this.input.text = trimMargin
    }

    override fun getBackground(): Color? {
        // it seems that the input field is not ready when this method is called
        if (this.input == null) return super.getBackground()

        val editor = input.editor ?: return super.getBackground()
        return editor.colorsScheme.defaultBackground
    }

    override fun setBackground(bg: Color?) {}

    fun addListener(listener: AutoDevInputListener) {
        editorListeners.addListener(listener)
    }

    private fun getInputValidationInfo(): ValidationInfo? {
        val text = input.getDocument().text
        val textLength = (this.tokenizer)?.count(text) ?: text.length

        val exceed: Int = textLength - AutoDevSettingsState.maxTokenLength
        if (exceed <= 0) return null

        val errorMessage = AutoDevBundle.message("chat.too.long.user.message", exceed)
        return ValidationInfo(errorMessage, this as JComponent).asWarning()
    }

    fun hasSelectedAgent(): Boolean {
        if (!project.customAgentSetting.enableCustomRag) return false
        if (customRag.selectedItem == null) return false
        return customRag.selectedItem != defaultRag
    }

    fun getSelectedAgent(): CustomAgentConfig {
        return customRag.selectedItem as CustomAgentConfig
    }

    fun resetAgent() {
        (customRag.selectedItem as? CustomAgentConfig)?.state.let {
            CustomAgentState.START
        }

        customRag.selectedItem = defaultRag
    }

    private val maxHeight: Int
        get() {
            val decorator: InternalDecorator = UIUtil.getParentOfType(
                InternalDecorator::class.java, this as Component
            )!!

            val contentManager: ContentManager = decorator.contentManager ?: return JBUI.scale(200)
            return contentManager.component.height / 2
        }

    val focusableComponent: JComponent get() = input
}

fun showPopupAbove(popup: JBPopup, component: Component) {
    val northWest = RelativePoint(component, Point())

    popup.addListener(object : JBPopupListener {
        override fun beforeShown(event: LightweightWindowEvent) {
            val location = Point(popup.locationOnScreen).apply { y = northWest.screenPoint.y - popup.size.height }

            popup.setLocation(location)
            popup.removeListener(this)
        }
    })

    popup.show(northWest)
}
