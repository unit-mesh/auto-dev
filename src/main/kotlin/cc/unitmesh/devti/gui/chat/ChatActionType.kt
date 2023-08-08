package cc.unitmesh.devti.gui.chat

import cc.unitmesh.devti.prompting.VcsPrompting
import com.intellij.openapi.components.service
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vcs.changes.ChangeListManagerImpl

enum class ChatActionType {
    CHAT,
    EXPLAIN_BUSINESS,
    REFACTOR,
    EXPLAIN,
    REVIEW,
    CODE_COMPLETE,
    GENERATE_TEST,
    GEN_COMMIT_MESSAGE,
    FIX_ISSUE,
    CREATE_DDL,
    CREATE_CHANGELOG,
    CUSTOM_COMPLETE,
    CUSTOM_ACTION
    ;

    override fun toString(): String {
        return instruction()
    }

    private fun prepareVcsContext(): String {
        val project = ProjectManager.getInstance().openProjects.firstOrNull() ?: return ""
        val changeListManager = ChangeListManagerImpl.getInstance(project)
        val changes = changeListManager.changeLists.flatMap {
            it.changes
        }

        val prompting = project.service<VcsPrompting>()
        return prompting.calculateDiff(changes, project)
    }

    val old_commit_prompt = """suggest 10 commit messages based on the following diff:
        commit messages should:
         - follow conventional commits
         - message format should be: <type>[scope]: <description>
        
        examples:
         - fix(authentication): add password regex pattern
         - feat(storage): add new test cases
         
         {{diff}}
         """.trimIndent()


    fun generateCommitMessage(diff: String): String {
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
            REVIEW -> "Code Review given following $lang code"
            REFACTOR -> "Refactor the given $lang code"
            CODE_COMPLETE -> "Complete $lang code, return rest code, no explaining"
            GENERATE_TEST -> "Write unit test for given $lang code"
            FIX_ISSUE -> "Help me fix this issue"
            GEN_COMMIT_MESSAGE -> {
                generateCommitMessage(prepareVcsContext())
            }

            CREATE_DDL -> "create ddl based on the given information"
            CREATE_CHANGELOG -> "generate release note"
            CHAT -> ""
            CUSTOM_COMPLETE -> ""
            CUSTOM_ACTION -> ""
            EXPLAIN_BUSINESS -> "Recover the original business scene and functionality of the provided code by describing it in a User Story format. Ensure that your explanation avoids any technical jargon or technology-related terms."
        }
    }
}
