// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package cc.unitmesh.devti.gui

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import org.apache.commons.lang3.StringUtils
import java.awt.BorderLayout
import java.awt.event.ActionEvent
import java.util.*
import javax.swing.BorderFactory
import javax.swing.ImageIcon
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel

class DevtiFlowToolWindowFactory : ToolWindowFactory, DumbAware {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val toolWindowContent = CalendarToolWindowContent(toolWindow)
        val content = ContentFactory.getInstance().createContent(toolWindowContent.contentPanel, "", false)
        toolWindow.contentManager.addContent(content)
    }

    private class CalendarToolWindowContent(toolWindow: ToolWindow) {
        val contentPanel = JPanel()
        private val currentDate = JLabel()
        private val timeZone = JLabel()
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
            setIconLabel(currentDate, CALENDAR_ICON_PATH)
            setIconLabel(timeZone, TIME_ZONE_ICON_PATH)
            setIconLabel(currentTime, TIME_ICON_PATH)
            calendarPanel.add(currentDate)
            calendarPanel.add(timeZone)
            calendarPanel.add(currentTime)
            return calendarPanel
        }

        private fun setIconLabel(label: JLabel, imagePath: String) {
            label.icon = ImageIcon(Objects.requireNonNull(javaClass.getResource(imagePath)))
        }

        private fun createControlsPanel(toolWindow: ToolWindow): JPanel {
            val controlsPanel = JPanel()
            val refreshDateAndTimeButton = JButton("Refresh")
            refreshDateAndTimeButton.addActionListener { e: ActionEvent? -> updateCurrentDateTime() }
            controlsPanel.add(refreshDateAndTimeButton)
            val hideToolWindowButton = JButton("Hide")
            hideToolWindowButton.addActionListener { e: ActionEvent? -> toolWindow.hide(null) }
            controlsPanel.add(hideToolWindowButton)
            return controlsPanel
        }

        private fun updateCurrentDateTime() {
            val calendar = Calendar.getInstance()
            currentDate.text = getCurrentDate(calendar)
            timeZone.text = getTimeZone(calendar)
            currentTime.text = getCurrentTime(calendar)
        }

        private fun getCurrentDate(calendar: Calendar): String {
            return (calendar[Calendar.DAY_OF_MONTH].toString() + "/"
                    + (calendar[Calendar.MONTH] + 1) + "/"
                    + calendar[Calendar.YEAR])
        }

        private fun getTimeZone(calendar: Calendar): String {
            val gmtOffset = calendar[Calendar.ZONE_OFFSET].toLong() // offset from GMT in milliseconds
            val gmtOffsetString = (gmtOffset / 3600000).toString()
            return if (gmtOffset > 0) "GMT + $gmtOffsetString" else "GMT - $gmtOffsetString"
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

        companion object {
            private const val CALENDAR_ICON_PATH = "/toolWindow/Calendar-icon.png"
            private const val TIME_ZONE_ICON_PATH = "/toolWindow/Time-zone-icon.png"
            private const val TIME_ICON_PATH = "/toolWindow/Time-icon.png"
        }
    }
}
