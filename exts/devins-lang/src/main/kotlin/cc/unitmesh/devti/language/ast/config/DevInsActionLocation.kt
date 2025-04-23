package cc.unitmesh.devti.language.ast.config

enum class DevInsActionLocation(val location: String, val description: String) {
    CONTEXT_MENU("ContextMenu", "Show in Context Menu by Right Click"),
    INTENTION_MENU("IntentionMenu", "Show in Intention Menu by Alt+Enter"),
    TERMINAL_MENU("TerminalMenu", "Show in Terminal panel menu bar"),
    COMMIT_MENU("CommitMenu", "Show in Commit panel menu bar"),
    RUN_PANEL("RunPanel", "Show in Run panel which is the bottom of the IDE"),
    INPUT_BOX("InputBox", "Show in Input Box"),
    DATABASE_MENU("DatabaseMenu", "Show in Database panel menu bar"),
    CONSOLE_MENU("ConsoleMenu", "Show in Console panel menu bar"),
    VCS_LOG_MENU("VcsLogMenu", "Show in VCS Log panel menu bar"),
    CHAT_BOX("ChatBox", "Show in Chat Box, default in Right Panel"),
    INLINE_CHAT("InlineChat", "Show in Inline Chat"),

    EXT_SONARQUBE_MENU("ExtSonarQubeMenu", "Show in SonarQube panel menu bar"),
    ;

    companion object {
        fun from(locationName: String): DevInsActionLocation {
            return fromLocationName(locationName) ?: RUN_PANEL
        }

        private fun fromLocationName(locationName: String): DevInsActionLocation? {
            return entries.firstOrNull { it.location == locationName }
        }

        fun all(): Array<DevInsActionLocation> = entries.toTypedArray()

        fun default(): String = RUN_PANEL.location
    }
}
