package cc.unitmesh.devti.gui.chat

import cc.unitmesh.devti.prompting.VcsPrompting
import com.intellij.openapi.components.service
import com.intellij.openapi.project.ProjectManager

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

    private fun prepareVcsContext(): String {
        val project = ProjectManager.getInstance().openProjects.firstOrNull() ?: return ""
        val prompting = project.service<VcsPrompting>()

        return prompting.prepareContext()
    }

    private fun generateCommitMessage(diff: String): String {
        return """Write a cohesive yet descriptive commit message for a given diff. 
Make sure to include both information What was changed and Why.
Start with a short sentence in imperative form, no more than 50 characters long.
Then leave an empty line and continue with a more detailed explanation, if necessary.
Explanation should have less than 200 characters.

examples:
- fix(authentication): add password regex pattern
- feat(storage): add new test cases

Diff:

```diff
$diff
```

"""
    }

    fun instruction(lang: String = ""): String {
        return when (this) {
            EXPLAIN -> "Explain selected $lang code"
            REFACTOR -> "Refactor the given $lang code"
            CODE_COMPLETE -> "Complete $lang code, return rest code, no explaining"
            GENERATE_TEST -> "Write unit test for given $lang code"
            FIX_ISSUE -> "Help me fix this issue"
            GEN_COMMIT_MESSAGE -> generateCommitMessage(prepareVcsContext())
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
