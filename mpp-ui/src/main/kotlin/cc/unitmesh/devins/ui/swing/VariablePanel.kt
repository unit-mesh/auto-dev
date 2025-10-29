package cc.unitmesh.devins.ui.swing

import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.*
import javax.swing.border.TitledBorder

class VariablePanel : JPanel() {
    
    private val variableListPanel = JPanel()
    private val scrollPane: JScrollPane
    private val variables = mutableMapOf<String, String>()
    
    init {
        layout = BorderLayout()
        border = TitledBorder("Variables")
        
        // 变量列表面板
        variableListPanel.layout = BoxLayout(variableListPanel, BoxLayout.Y_AXIS)
        scrollPane = JScrollPane(variableListPanel)
        scrollPane.verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        scrollPane.horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        
        add(scrollPane, BorderLayout.CENTER)
        
        // 添加变量按钮
        val buttonPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        val addButton = JButton("Add Variable")
        addButton.addActionListener {
            addNewVariable()
        }
        buttonPanel.add(addButton)
        add(buttonPanel, BorderLayout.SOUTH)
        
        refreshUI()
    }

    fun addVariable(key: String, value: String) {
        variables[key] = value
        refreshUI()
    }

    fun removeVariable(key: String) {
        variables.remove(key)
        refreshUI()
    }
    
    fun getVariables(): Map<String, String> {
        return variables.toMap()
    }
    
    private fun addNewVariable() {
        val key = JOptionPane.showInputDialog(
            this,
            "Enter variable name:",
            "Add Variable",
            JOptionPane.PLAIN_MESSAGE
        )
        
        if (!key.isNullOrBlank()) {
            val cleanKey = key.trim()
            if (cleanKey.isNotEmpty()) {
                variables[cleanKey] = ""
                refreshUI()
            }
        }
    }
    
    private fun refreshUI() {
        variableListPanel.removeAll()
        
        if (variables.isEmpty()) {
            val emptyLabel = JLabel("No variables defined. Click 'Add Variable' to add one.")
            emptyLabel.foreground = Color.GRAY
            emptyLabel.horizontalAlignment = SwingConstants.CENTER
            variableListPanel.add(emptyLabel)
        } else {
            variables.forEach { (key, value) ->
                val variableComponent = createVariableComponent(key, value)
                variableListPanel.add(variableComponent)
                variableListPanel.add(Box.createVerticalStrut(5))
            }
        }
        
        variableListPanel.revalidate()
        variableListPanel.repaint()
    }
    
    private fun createVariableComponent(key: String, value: String): JComponent {
        val panel = JPanel(BorderLayout())
        panel.border = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.LIGHT_GRAY),
            BorderFactory.createEmptyBorder(8, 8, 8, 8)
        )
        panel.background = Color.WHITE
        
        // 变量名标签
        val keyLabel = JLabel("$${key}")
        keyLabel.font = keyLabel.font.deriveFont(Font.BOLD)
        keyLabel.foreground = Color.BLUE
        
        // 删除按钮
        val deleteButton = JButton("×")
        deleteButton.preferredSize = Dimension(25, 25)
        deleteButton.font = deleteButton.font.deriveFont(Font.BOLD, 14f)
        deleteButton.foreground = Color.RED
        deleteButton.addActionListener {
            removeVariable(key)
        }
        
        // 顶部面板（变量名 + 删除按钮）
        val topPanel = JPanel(BorderLayout())
        topPanel.add(keyLabel, BorderLayout.WEST)
        topPanel.add(deleteButton, BorderLayout.EAST)
        topPanel.background = Color.WHITE
        
        // 值输入框
        val valueField = JTextField(value)
        valueField.addActionListener {
            variables[key] = valueField.text
        }
        valueField.document.addDocumentListener(object : javax.swing.event.DocumentListener {
            override fun insertUpdate(e: javax.swing.event.DocumentEvent?) {
                variables[key] = valueField.text
            }
            
            override fun removeUpdate(e: javax.swing.event.DocumentEvent?) {
                variables[key] = valueField.text
            }
            
            override fun changedUpdate(e: javax.swing.event.DocumentEvent?) {
                variables[key] = valueField.text
            }
        })
        
        panel.add(topPanel, BorderLayout.NORTH)
        panel.add(Box.createVerticalStrut(5), BorderLayout.CENTER)
        panel.add(valueField, BorderLayout.SOUTH)
        
        return panel
    }
}
