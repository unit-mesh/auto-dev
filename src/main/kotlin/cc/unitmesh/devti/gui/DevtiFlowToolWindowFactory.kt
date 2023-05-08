// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package cc.unitmesh.devti.gui

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

class DevtiFlowToolWindowFactory : ToolWindowFactory, DumbAware {
    private val contentFactory = ApplicationManager.getApplication().getService(
        ContentFactory::class.java
    )

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val chatCodingService = ChatCodingService()
        val contentPanel = ChatCodingComponent(chatCodingService)
        val content = contentFactory.createContent(contentPanel, "", false)
        toolWindow.contentManager.addContent(content)
    }

}
