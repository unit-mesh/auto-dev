package cc.unitmesh.devins.idea.terminal

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project

/**
 * Compatibility layer for IntelliJ Terminal API.
 * 
 * Supports both:
 * - 2025.2: Uses reflection to access old Terminal API
 * - 2025.3+: Uses new TerminalToolWindowTabsManager API
 * 
 * Based on IntelliJ Platform Plugin SDK documentation:
 * https://plugins.jetbrains.com/docs/intellij/embedded-terminal.html
 */
object TerminalApiCompat {
    private val LOG = logger<TerminalApiCompat>()
    
    /**
     * Opens a command in IDEA's native terminal.
     * 
     * @param project The current project
     * @param command The command to execute
     * @param tabName Optional custom tab name (defaults to "AutoDev: <command>")
     * @param requestFocus Whether to focus the terminal tab
     * @return true if successful, false otherwise
     */
    fun openCommandInTerminal(
        project: Project,
        command: String,
        tabName: String? = null,
        requestFocus: Boolean = true
    ): Boolean {
        return try {
            // Try new API first (2025.3+)
            tryNewTerminalApi(project, command, tabName, requestFocus)
        } catch (e: ClassNotFoundException) {
            LOG.info("New Terminal API not available, trying fallback approach")
            // Fallback: Try old API or alternative approach
            tryFallbackTerminalApi(project, command, tabName, requestFocus)
        } catch (e: Exception) {
            LOG.warn("Failed to open command in terminal: ${e.message}", e)
            false
        }
    }
    
    /**
     * Try using the new Terminal API (2025.3+).
     * Uses TerminalToolWindowTabsManager from com.intellij.terminal.frontend.toolwindow
     */
    private fun tryNewTerminalApi(
        project: Project,
        command: String,
        tabName: String?,
        requestFocus: Boolean
    ): Boolean {
        try {
            // Load classes using reflection to avoid compile-time dependency
            val managerClass = Class.forName("com.intellij.terminal.frontend.toolwindow.TerminalToolWindowTabsManager")
            val getInstanceMethod = managerClass.getMethod("getInstance", Project::class.java)
            val manager = getInstanceMethod.invoke(null, project)
            
            // Create tab builder
            val createTabBuilderMethod = managerClass.getMethod("createTabBuilder")
            val tabBuilder = createTabBuilderMethod.invoke(manager)
            val tabBuilderClass = tabBuilder.javaClass
            
            // Configure tab
            val effectiveTabName = tabName ?: "AutoDev: $command"
            val tabNameMethod = tabBuilderClass.getMethod("tabName", String::class.java)
            tabNameMethod.invoke(tabBuilder, effectiveTabName)
            
            val requestFocusMethod = tabBuilderClass.getMethod("requestFocus", Boolean::class.javaPrimitiveType)
            requestFocusMethod.invoke(tabBuilder, requestFocus)
            
            // Create tab
            val createTabMethod = tabBuilderClass.getMethod("createTab")
            val tab = createTabMethod.invoke(tabBuilder)
            
            // Get view and send text
            val tabClass = tab.javaClass
            val getViewMethod = tabClass.getMethod("getView")
            val view = getViewMethod.invoke(tab)
            
            val viewClass = view.javaClass
            val sendTextMethod = viewClass.getMethod("sendText", String::class.java)
            sendTextMethod.invoke(view, command + "\n")
            
            LOG.info("Successfully opened command in terminal using new API: $command")
            return true
        } catch (e: NoSuchMethodException) {
            LOG.warn("New Terminal API method not found: ${e.message}")
            throw e
        }
    }
    
    /**
     * Fallback approach for older IDEA versions or when new API is not available.
     * Uses TerminalToolWindowManager to create shell widget and execute command.
     */
    private fun tryFallbackTerminalApi(
        project: Project,
        command: String,
        tabName: String?,
        requestFocus: Boolean
    ): Boolean {
        try {
            // Try to use TerminalToolWindowManager (Classic Terminal API)
            val managerClass = Class.forName("org.jetbrains.plugins.terminal.TerminalToolWindowManager")
            val getInstanceMethod = managerClass.getMethod("getInstance", Project::class.java)
            val manager = getInstanceMethod.invoke(null, project)

            // Create shell widget
            val effectiveTabName = tabName ?: "AutoDev: $command"
            val createShellWidgetMethod = managerClass.getMethod(
                "createShellWidget",
                String::class.java,  // workingDirectory
                String::class.java,  // tabName
                Boolean::class.javaPrimitiveType,  // requestFocus
                Boolean::class.javaPrimitiveType   // deferSessionStartUntilUiShown
            )

            val widget = createShellWidgetMethod.invoke(
                manager,
                null,  // workingDirectory (use default)
                effectiveTabName,
                requestFocus,
                false  // don't defer, start immediately
            )

            // Execute command on the widget
            val widgetClass = widget.javaClass
            val executeCommandMethod = widgetClass.getMethod("executeCommand", String::class.java)
            executeCommandMethod.invoke(widget, command)

            LOG.info("Successfully executed command in terminal using fallback API: $command")
            return true
        } catch (e: ClassNotFoundException) {
            LOG.warn("TerminalToolWindowManager not found, trying basic activation")
            return tryBasicTerminalActivation(project, requestFocus)
        } catch (e: NoSuchMethodException) {
            LOG.warn("Terminal API method not found: ${e.message}")
            return tryBasicTerminalActivation(project, requestFocus)
        } catch (e: Exception) {
            LOG.warn("Fallback terminal API failed: ${e.message}", e)
            return tryBasicTerminalActivation(project, requestFocus)
        }
    }

    /**
     * Last resort: just activate the terminal tool window without executing command.
     */
    private fun tryBasicTerminalActivation(project: Project, requestFocus: Boolean): Boolean {
        return try {
            val toolWindowManager = com.intellij.openapi.wm.ToolWindowManager.getInstance(project)
            val terminalToolWindow = toolWindowManager.getToolWindow("Terminal")

            if (terminalToolWindow != null && requestFocus) {
                terminalToolWindow.activate(null)
                LOG.info("Activated Terminal tool window (command not sent)")
                return true
            }

            LOG.warn("Terminal tool window not found")
            false
        } catch (e: Exception) {
            LOG.warn("Basic terminal activation failed: ${e.message}", e)
            false
        }
    }
    
    /**
     * Check if Terminal API is available in the current IDEA version.
     */
    fun isTerminalApiAvailable(project: Project): Boolean {
        return try {
            val toolWindowManager = com.intellij.openapi.wm.ToolWindowManager.getInstance(project)
            toolWindowManager.getToolWindow("Terminal") != null
        } catch (e: Exception) {
            false
        }
    }
}

