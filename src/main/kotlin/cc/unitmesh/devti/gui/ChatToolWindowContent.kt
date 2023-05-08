package cc.unitmesh.devti.gui

import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.components.JBTextField
import org.apache.commons.lang3.StringUtils
import java.awt.BorderLayout
import java.util.*
import javax.swing.BorderFactory
import javax.swing.JLabel
import javax.swing.JPanel

class ChatToolWindowContent(toolWindow: ToolWindow) {
    val contentPanel = JPanel()
    private val currentTime = JLabel()
    val storyId = JBTextField()

    init {
        contentPanel.layout = BorderLayout(0, 20)
        contentPanel.border = BorderFactory.createEmptyBorder(40, 0, 0, 0)
        contentPanel.add(createCalendarPanel(), BorderLayout.PAGE_START)
        contentPanel.add(createControlsPanel(toolWindow), BorderLayout.CENTER)
        updateCurrentDateTime()
    }

    private fun createCalendarPanel(): JPanel {
        val calendarPanel = JPanel()
        LabeledComponent.create(storyId, "Story ID").apply {
            calendarPanel.add(this)
        }
        // code review button
        return calendarPanel
    }

    private fun createControlsPanel(toolWindow: ToolWindow): JPanel {
        val controlsPanel = JPanel()
        return controlsPanel
    }

    private fun updateCurrentDateTime() {
        val calendar = Calendar.getInstance()
        currentTime.text = getCurrentTime(calendar)
    }

    private fun getCurrentTime(calendar: Calendar): String {
        return getFormattedValue(calendar, Calendar.HOUR_OF_DAY) + ":" + getFormattedValue(
            calendar,
            Calendar.MINUTE
        )
    }

    private fun getFormattedValue(calendar: Calendar, calendarField: Int): String {
        val value = calendar[calendarField]
        return StringUtils.leftPad(Integer.toString(value), 2, "0")
    }
}