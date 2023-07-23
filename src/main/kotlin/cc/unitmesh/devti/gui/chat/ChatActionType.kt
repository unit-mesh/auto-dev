package cc.unitmesh.devti.gui.chat

enum class ChatActionType {
    REFACTOR,
    EXPLAIN,
    REVIEW,
    CODE_COMPLETE,
    WRITE_TEST,
    GEN_COMMIT_MESSAGE,
    FIX_ISSUE,
    CREATE_DDL,
    CREATE_CHANGELOG;

    fun instruction(): String {
        return when (this) {
            EXPLAIN -> "Explain selected code"
            REVIEW -> "Code Review"
            REFACTOR -> "Refactor the following code"
            CODE_COMPLETE -> "Complete java code, return rest code, no explaining"
            WRITE_TEST -> "Write test for following code"
            FIX_ISSUE -> "Help me fix this issue"
            GEN_COMMIT_MESSAGE -> "generate commit message"
            CREATE_DDL -> "create ddl"
            CREATE_CHANGELOG -> "generate release note"
        }
    }
}
