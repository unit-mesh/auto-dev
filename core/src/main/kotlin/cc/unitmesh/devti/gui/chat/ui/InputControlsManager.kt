package cc.unitmesh.devti.gui.chat.ui

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.AutoDevIcons
import cc.unitmesh.devti.AutoDevNotifications
import cc.unitmesh.devti.indexer.DomainDictService
import cc.unitmesh.devti.indexer.usage.PromptEnhancer
import cc.unitmesh.devti.llms.tokenizer.Tokenizer
import cc.unitmesh.devti.llms.tokenizer.TokenizerFactory
import cc.unitmesh.devti.settings.AutoDevSettingsState
import cc.unitmesh.devti.util.AutoDevCoroutineScope
import cc.unitmesh.devti.util.parser.CodeFence
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComponentValidator
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.util.EventDispatcher
import com.intellij.util.ui.JBEmptyBorder
import com.intellij.util.ui.JBUI
import kotlinx.coroutines.launch
import java.awt.*
import java.util.function.Supplier
import javax.swing.*

/**
 * Manages input field, buttons, and UI controls for AutoDevInputSection
 */
class InputControlsManager(
    private val project: Project,
    private val disposable: Disposable?,
    private val editorListeners: EventDispatcher<AutoDevInputListener>
) {
    private val logger = logger<InputControlsManager>()
    
    // Input components
    lateinit var input: AutoDevInput
        private set
    
    // Button components
    private lateinit var sendButtonPresentation: Presentation
    private lateinit var stopButtonPresentation: Presentation
    private lateinit var enhanceButtonPresentation: Presentation
    lateinit var sendButton: ActionButton
        private set
    lateinit var stopButton: ActionButton
        private set
    lateinit var enhanceButton: ActionButton
        private set
    lateinit var buttonPanel: JPanel
        private set
    
    // Document listener
    private lateinit var documentListener: DocumentListener
    
    // Tokenizer for validation
    private var tokenizer: Tokenizer? = try {
        lazy { TokenizerFactory.createTokenizer() }.value
    } catch (e: Exception) {
        logger.error("TokenizerImpl.INSTANCE is not available", e)
        null
    }
    
    fun initialize(inputSection: AutoDevInputSection) {
        createInput(inputSection)
        createButtons(inputSection)
        setupDocumentListener()
        setupValidation(inputSection)
    }
    
    private fun createInput(inputSection: AutoDevInputSection) {
        input = AutoDevInput(project, listOf(), disposable, inputSection)
        input.border = JBEmptyBorder(10)
        input.minimumSize = Dimension(input.minimumSize.width, 64)
    }
    
    private fun createButtons(inputSection: AutoDevInputSection) {
        // Create presentations
        sendButtonPresentation = Presentation(AutoDevBundle.message("chat.panel.send")).apply {
            icon = AutoDevIcons.SEND
        }
        
        stopButtonPresentation = Presentation(AutoDevBundle.message("chat.panel.stop")).apply {
            icon = AutoDevIcons.STOP
        }
        
        enhanceButtonPresentation = Presentation(AutoDevBundle.message("chat.panel.enhance")).apply {
            icon = AutoDevIcons.MAGIC
            isEnabled = project.service<DomainDictService>().loadContent()?.isNotEmpty() == true
        }
        
        // Create buttons
        sendButton = ActionButton(
            DumbAwareAction.create {
                editorListeners.multicaster.onSubmit(inputSection, AutoDevInputTrigger.Button)
            },
            sendButtonPresentation, "", Dimension(20, 20)
        )
        
        stopButton = ActionButton(
            DumbAwareAction.create {
                editorListeners.multicaster.onStop(inputSection)
            },
            stopButtonPresentation, "", Dimension(20, 20)
        )
        
        enhanceButton = ActionButton(
            DumbAwareAction.create {
                AutoDevCoroutineScope.scope(project).launch {
                    enhancePrompt()
                }
            },
            enhanceButtonPresentation, "", Dimension(20, 20)
        )
        
        buttonPanel = createButtonPanel()
    }
    
    private fun createButtonPanel(): JPanel {
        val panel = JPanel(CardLayout())
        
        // Create a panel for the "Send" state that contains both enhance and send buttons
        val sendPanel = JPanel(FlowLayout(FlowLayout.RIGHT, JBUI.scale(8), 0))
        sendPanel.isOpaque = false
        sendPanel.add(enhanceButton)
        sendPanel.add(sendButton)
        
        panel.add(sendPanel, "Send")
        panel.add(stopButton, "Stop")
        
        return panel
    }
    
    private fun setupDocumentListener() {
        documentListener = object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                val i = input.preferredSize?.height
                if (i != input.height) {
                    input.parent?.revalidate()
                }
            }
        }
        
        input.addDocumentListener(documentListener)
        input.recreateDocument()
    }
    
    private fun setupValidation(inputSection: AutoDevInputSection) {
        ComponentValidator(disposable!!).withValidator(Supplier<ValidationInfo?> {
            val validationInfo: ValidationInfo? = getInputValidationInfo(inputSection)
            sendButton.setEnabled(validationInfo == null)
            return@Supplier validationInfo
        }).installOn(inputSection as JComponent).revalidate()
    }
    
    private suspend fun enhancePrompt() {
        val originalIcon = enhanceButtonPresentation.icon
        enhanceButtonPresentation.icon = AutoDevIcons.LOADING
        enhanceButtonPresentation.isEnabled = false
        
        try {
            val content = project.service<PromptEnhancer>().create(input.text)
            val code = CodeFence.parse(content).text
            runInEdt {
                input.text = code
            }
        } catch (e: Exception) {
            logger.error("Failed to enhance prompt", e)
            AutoDevNotifications.error(project, e.message ?: "An error occurred while enhancing the prompt")
        } finally {
            enhanceButtonPresentation.icon = originalIcon
            enhanceButtonPresentation.isEnabled = true
        }
    }
    
    private fun getInputValidationInfo(inputSection: AutoDevInputSection): ValidationInfo? {
        val text = input.document.text
        val textLength = (this.tokenizer)?.count(text) ?: text.length
        
        val exceed: Int = textLength - AutoDevSettingsState.maxTokenLength
        if (exceed <= 0) return null
        
        val errorMessage = AutoDevBundle.message("chat.too.long.user.message", exceed)
        return ValidationInfo(errorMessage, inputSection as JComponent).asWarning()
    }
    
    // Public API methods
    fun renderText(): String {
        return input.text
    }
    
    fun clearText() {
        input.recreateDocument()
        input.text = ""
    }
    
    fun setText(text: String) {
        input.recreateDocument()
        input.text = text
    }
    
    fun setContent(text: String) {
        val focusManager = IdeFocusManager.getInstance(project)
        focusManager.requestFocus(input, true)
        input.recreateDocument()
        input.text = text
    }
    
    fun showStopButton() {
        (buttonPanel.layout as? CardLayout)?.show(buttonPanel, "Stop")
        stopButton.isEnabled = true
    }
    
    fun showSendButton() {
        (buttonPanel.layout as? CardLayout)?.show(buttonPanel, "Send")
        buttonPanel.isEnabled = true
    }
    
    fun send() {
        // This will be handled by the button action
    }
    
    fun moveCursorToStart() {
        runInEdt {
            input.requestFocus()
            input.caretModel.moveToOffset(0)
        }
    }
    
    fun getFocusableComponent(): JComponent = input
}
