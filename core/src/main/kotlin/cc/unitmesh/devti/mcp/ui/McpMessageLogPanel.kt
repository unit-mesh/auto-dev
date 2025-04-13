package cc.unitmesh.devti.mcp.ui

import cc.unitmesh.devti.mcp.ui.model.McpMessage
import cc.unitmesh.devti.mcp.ui.model.MessageType
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.time.format.DateTimeFormatter
import javax.swing.*
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel

class McpMessageLogPanel : JPanel(BorderLayout()) {
    private val messages = mutableListOf<McpMessage>()
    private val tableModel = MessageTableModel()
    private val table = JBTable(tableModel).apply {
        setShowGrid(false)
        intercellSpacing = Dimension(0, 0)
        rowHeight = 30
        selectionModel = DefaultListSelectionModel().apply {
            selectionMode = ListSelectionModel.SINGLE_SELECTION
        }
        autoCreateRowSorter = true
    }

    private val detailCardLayout = CardLayout()
    private val detailPanel = JPanel(detailCardLayout)

    private val requestDetailPanel = RequestDetailPanel()
    
    private val responseDetailPanel = ResponseDetailPanel()
    
    companion object {
        private const val REQUEST_PANEL = "REQUEST_PANEL"
        private const val RESPONSE_PANEL = "RESPONSE_PANEL"
        private const val EMPTY_PANEL = "EMPTY_PANEL"
    }

    init {
        table.getColumnModel().getColumn(0).cellRenderer = TypeColumnRenderer()
        
        detailPanel.add(JPanel(), EMPTY_PANEL)
        detailPanel.add(requestDetailPanel, REQUEST_PANEL)
        detailPanel.add(responseDetailPanel, RESPONSE_PANEL)

        val splitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT).apply {
            leftComponent = JBScrollPane(table)
            rightComponent = JBScrollPane(detailPanel)
            dividerLocation = 600
            resizeWeight = 0.5
        }

        add(splitPane, BorderLayout.CENTER)
        table.selectionModel.addListSelectionListener { e ->
            if (!e.valueIsAdjusting && table.selectedRow >= 0) {
                val selectedIndex = table.convertRowIndexToModel(table.selectedRow)
                if (selectedIndex >= 0 && selectedIndex < messages.size) {
                    val message = messages[selectedIndex]
                    when (message.type) {
                        MessageType.REQUEST -> {
                            requestDetailPanel.displayMessage(message)
                            detailCardLayout.show(detailPanel, REQUEST_PANEL)
                        }
                        MessageType.RESPONSE -> {
                            responseDetailPanel.displayMessage(message)
                            detailCardLayout.show(detailPanel, RESPONSE_PANEL)
                        }
                    }
                } else {
                    detailCardLayout.show(detailPanel, EMPTY_PANEL)
                }
            }
        }
    }

    fun addMessage(message: McpMessage) {
        messages.add(message)
        tableModel.fireTableDataChanged()
        SwingUtilities.invokeLater {
            table.setRowSelectionInterval(messages.size - 1, messages.size - 1)
        }
    }

    fun clear() {
        messages.clear()
        tableModel.fireTableDataChanged()
        detailCardLayout.show(detailPanel, EMPTY_PANEL)
    }

    private inner class MessageTableModel : DefaultTableModel() {
        private val columnNames = arrayOf(
            "Type",
            "Method",
            "Timestamp",
            "Duration"
        )

        override fun getColumnCount(): Int = columnNames.size

        override fun getRowCount(): Int = messages.size

        override fun getColumnName(column: Int): String = columnNames[column]

        override fun getValueAt(row: Int, column: Int): Any {
            val message = messages[row]
            return when (column) {
                0 -> message.type
                1 -> message.method
                2 -> message.timestamp.format(DateTimeFormatter.ofPattern("HH:mm:ss"))
                3 -> message.duration?.toString() ?: "-"
                else -> ""
            }
        }

        override fun isCellEditable(row: Int, column: Int): Boolean = false
    }

    private class TypeColumnRenderer : DefaultTableCellRenderer() {
        override fun getTableCellRendererComponent(
            table: JTable?,
            value: Any?,
            isSelected: Boolean,
            hasFocus: Boolean,
            row: Int,
            column: Int
        ): Component {
            val label = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column) as JLabel
            label.horizontalAlignment = CENTER

            if (!isSelected) {
                when (value) {
                    MessageType.REQUEST -> {
                        label.background = JBColor(0xE1F5FE, 0x0D47A1) // Light blue for request
                        label.foreground = JBColor(0x01579B, 0xE3F2FD)
                    }
                    MessageType.RESPONSE -> {
                        label.background = JBColor(0xE8F5E9, 0x2E7D32) // Light green for response
                        label.foreground = JBColor(0x1B5E20, 0xE8F5E9)
                    }
                }
            }

            label.text = when (value) {
                MessageType.REQUEST -> "REQUEST"
                MessageType.RESPONSE -> "RESPONSE"
                else -> ""
            }

            label.border = JBUI.Borders.empty(3, 5)

            return label
        }
    }

    private class RequestDetailPanel : JPanel(BorderLayout()) {
        private val headerPanel = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(10, 10, 5, 10)
            background = JBColor(0xF8F9FA, 0x2B2D30)
        }
        
        private val toolLabel = JBLabel().apply {
            font = font.deriveFont(Font.BOLD, font.size + 2f)
        }
        
        private val parametersPanel = JPanel(GridBagLayout()).apply {
            border = JBUI.Borders.empty(10)
            background = JBColor(0xFFFFFF, 0x2B2D30)
        }
        
        init {
            headerPanel.add(JBLabel("Tool:").apply { 
                font = font.deriveFont(Font.BOLD) 
                border = JBUI.Borders.emptyRight(8)
            }, BorderLayout.WEST)
            headerPanel.add(toolLabel, BorderLayout.CENTER)
            
            add(headerPanel, BorderLayout.NORTH)
            add(JBScrollPane(parametersPanel), BorderLayout.CENTER)
        }
        
        fun displayMessage(message: McpMessage) {
            toolLabel.text = message.toolName ?: "Unknown Tool"
            
            parametersPanel.removeAll()
            
            val paramJson = message.parameters
            if (paramJson != null && paramJson != "{}" && paramJson.isNotBlank()) {
                try {
                    val json = Json { ignoreUnknownKeys = true }
                    val parsedJson = json.parseToJsonElement(paramJson).jsonObject
                    
                    val headerConstraints = GridBagConstraints().apply {
                        gridx = 0
                        gridy = 0
                        gridwidth = 2
                        fill = GridBagConstraints.HORIZONTAL
                        anchor = GridBagConstraints.NORTHWEST
                        insets = JBUI.insetsBottom(10)
                    }
                    
                    parametersPanel.add(JBLabel("Parameters:").apply { 
                        font = font.deriveFont(Font.BOLD, font.size + 1f)
                    }, headerConstraints)
                    
                    var row = 1
                    parsedJson.entries.forEach { (key, value) ->
                        // Parameter name
                        val nameConstraints = GridBagConstraints().apply {
                            gridx = 0
                            gridy = row
                            anchor = GridBagConstraints.NORTHWEST
                            insets = JBUI.insets(5, 0, 5, 10)
                        }
                        
                        parametersPanel.add(JBLabel("$key:").apply { 
                            font = font.deriveFont(Font.BOLD) 
                        }, nameConstraints)
                        
                        // Parameter value
                        val valueConstraints = GridBagConstraints().apply {
                            gridx = 1
                            gridy = row++
                            weightx = 1.0
                            fill = GridBagConstraints.HORIZONTAL
                            anchor = GridBagConstraints.NORTHWEST
                            insets = JBUI.insets(5, 0)
                        }
                        
                        val valueText = formatJsonValue(value)
                        val valueTextArea = JTextArea(valueText).apply {
                            lineWrap = true
                            wrapStyleWord = true
                            isEditable = false
                            border = null
                            background = parametersPanel.background
                        }
                        
                        parametersPanel.add(valueTextArea, valueConstraints)
                    }
                    
                    // Add filler to push everything to the top
                    val fillerConstraints = GridBagConstraints().apply {
                        gridx = 0
                        gridy = row
                        gridwidth = 2
                        weighty = 1.0
                        fill = GridBagConstraints.BOTH
                    }
                    parametersPanel.add(JPanel().apply { background = parametersPanel.background }, fillerConstraints)
                    
                } catch (e: Exception) {
                    // If parsing fails, fall back to displaying raw JSON
                    val rawParamConstraints = GridBagConstraints().apply {
                        gridx = 0
                        gridy = 0
                        weightx = 1.0
                        weighty = 1.0
                        fill = GridBagConstraints.BOTH
                    }
                    
                    parametersPanel.add(JTextArea(paramJson).apply {
                        lineWrap = true
                        wrapStyleWord = true
                        isEditable = false
                    }, rawParamConstraints)
                }
            } else {
                val noParamsConstraints = GridBagConstraints().apply {
                    gridx = 0
                    gridy = 0
                    weightx = 1.0
                    fill = GridBagConstraints.HORIZONTAL
                }
                
                parametersPanel.add(JBLabel("No parameters").apply {
                    foreground = JBColor.GRAY
                }, noParamsConstraints)
                
                val fillerConstraints = GridBagConstraints().apply {
                    gridx = 0
                    gridy = 1
                    weightx = 1.0
                    weighty = 1.0
                    fill = GridBagConstraints.BOTH
                }
                parametersPanel.add(JPanel().apply { background = parametersPanel.background }, fillerConstraints)
            }
            
            parametersPanel.revalidate()
            parametersPanel.repaint()
        }
        
        private fun formatJsonValue(element: JsonElement): String {
            // Convert JsonElement to a nicely formatted string
            return element.toString()
        }
    }

    private class ResponseDetailPanel : JPanel(BorderLayout()) {
        private val headerPanel = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            border = JBUI.Borders.empty(10)
            background = JBColor(0xF8F9FA, 0x2B2D30)
        }
        
        private val durationLabel = JBLabel().apply {
            font = font.deriveFont(Font.BOLD)
        }
        
        private val contentTextArea = JTextArea().apply {
            isEditable = false
            wrapStyleWord = true
            lineWrap = true
            border = JBUI.Borders.empty(10)
        }
        
        init {
            headerPanel.add(JBLabel("Response Duration:"))
            headerPanel.add(durationLabel)
            
            add(headerPanel, BorderLayout.NORTH)
            add(JBScrollPane(contentTextArea), BorderLayout.CENTER)
        }
        
        fun displayMessage(message: McpMessage) {
            durationLabel.text = message.duration?.toString()?.plus(" ms") ?: "N/A"
            contentTextArea.text = message.content
            contentTextArea.caretPosition = 0
        }
    }
}
