// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package cc.unitmesh.devti.gui

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import org.apache.commons.lang3.StringUtils
import java.awt.BorderLayout
import java.util.*
import javax.swing.BorderFactory
import javax.swing.JLabel
import javax.swing.JPanel

class DevtiFlowToolWindowFactory : ToolWindowFactory, DumbAware {
    private val contentFactory = ApplicationManager.getApplication().getService(
        ContentFactory::class.java
    )

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val toolWindowContent = CalendarToolWindowContent(toolWindow)
        val content = contentFactory.createContent(toolWindowContent.contentPanel, "", false)
        toolWindow.contentManager.addContent(content)
    }

    private class CalendarToolWindowContent(toolWindow: ToolWindow) {
        val contentPanel = JPanel()
        private val currentTime = JLabel()

        init {
            contentPanel.layout = BorderLayout(0, 20)
            contentPanel.border = BorderFactory.createEmptyBorder(40, 0, 0, 0)
            contentPanel.add(createCalendarPanel(), BorderLayout.PAGE_START)
            contentPanel.add(createControlsPanel(toolWindow), BorderLayout.CENTER)
            updateCurrentDateTime()
        }

        private fun createCalendarPanel(): JPanel {
            val calendarPanel = JPanel()
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
}
