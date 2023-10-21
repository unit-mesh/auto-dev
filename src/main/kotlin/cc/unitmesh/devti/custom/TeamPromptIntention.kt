package cc.unitmesh.devti.custom

import cc.unitmesh.devti.custom.team.TeamPromptAction
import cc.unitmesh.devti.intentions.action.base.AbstractChatIntention

class TeamPromptIntention(val intentionConfig: TeamPromptAction) : AbstractChatIntention() {
    companion object {
        fun create(intentionConfig: TeamPromptAction): TeamPromptIntention {
            return TeamPromptIntention(intentionConfig)
        }
    }

    override fun priority(): Int {
        return intentionConfig.actionPrompt.priority
    }

    override fun getText(): String {
        return intentionConfig.actionName
    }

    override fun getFamilyName(): String {
        return intentionConfig.actionName
    }
}
