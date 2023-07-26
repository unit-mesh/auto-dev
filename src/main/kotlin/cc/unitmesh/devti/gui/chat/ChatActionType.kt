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

    override fun toString(): String {
        return when (this) {
            EXPLAIN -> "explain"
            REVIEW -> "review"
            REFACTOR -> "refactor"
            CODE_COMPLETE -> "code complete"
            WRITE_TEST -> "write test"
            FIX_ISSUE -> "fix issue"
            GEN_COMMIT_MESSAGE -> "generate commit message"
            CREATE_DDL -> "create ddl"
            CREATE_CHANGELOG -> "generate release note"
        }
    }

    fun instruction(lang: String = ""): String {
        return when (this) {
            EXPLAIN -> "Explain selected $lang code"
            REVIEW -> "Code Review for following $lang code"
            REFACTOR -> "Refactor the following $lang code"
            CODE_COMPLETE -> "Complete $lang  code, return rest code, no explaining"
            WRITE_TEST -> "Write unit test for following $lang code"
            FIX_ISSUE -> "Help me fix this issue"
            GEN_COMMIT_MESSAGE -> """suggest 10 commit messages based on the following diff:
commit messages should:
 - follow conventional commits
 - message format should be: <type>[scope]: <description>

examples:
 - fix(authentication): add password regex pattern
 - feat(storage): add new test cases
 
 {{diff}}
 """

            CREATE_DDL -> "create ddl"
            CREATE_CHANGELOG -> "generate release note"
        }
    }
}
