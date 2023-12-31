package cc.unitmesh.devti.gui.chat

enum class ChatActionType {
    CHAT,
    REFACTOR,
    EXPLAIN,
    CODE_COMPLETE,
    GENERATE_TEST,
    GENERATE_TEST_DATA,
    GEN_COMMIT_MESSAGE,
    FIX_ISSUE,
    CREATE_CHANGELOG,
    CREATE_GENIUS,
    CUSTOM_COMPLETE,
    CUSTOM_ACTION,
    COUNIT,
    CODE_REVIEW
    ;

    override fun toString(): String {
        return instruction()
    }
    fun instruction(lang: String = ""): String {
        return when (this) {
            EXPLAIN -> "Explain selected $lang code"
            REFACTOR -> "Refactor the given $lang code"
            CODE_COMPLETE -> "Complete $lang code, return rest code, no explaining"
            GENERATE_TEST -> "Write unit test for given $lang code"
            FIX_ISSUE -> "Help me fix this issue"
            GEN_COMMIT_MESSAGE -> ""
            CREATE_CHANGELOG -> "generate release note"
            CHAT -> ""
            CUSTOM_COMPLETE -> ""
            CUSTOM_ACTION -> ""
            COUNIT -> ""
            CODE_REVIEW -> ""
            CREATE_GENIUS -> ""
            GENERATE_TEST_DATA -> "Generate JSON for given $lang code. So that we can use it to test for APIs. \n" +
                    "Make sure JSON contains real business logic, not just data structure. \n" +
                    "For example, if the code is a function that returns a list of users, " +
                    "the JSON should contain a list of users, not just a list of user objects."
        }
    }
}
