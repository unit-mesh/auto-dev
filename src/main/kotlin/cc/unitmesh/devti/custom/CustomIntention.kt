package cc.unitmesh.devti.custom

import cc.unitmesh.devti.intentions.AbstractChatIntention

class CustomIntention(val intentionConfig: CustomIntentionConfig) : AbstractChatIntention() {

    override fun getText(): String = intentionConfig.title

    override fun getFamilyName(): String = "Custom Intention"

    companion object {
        fun create(intentionConfig: CustomIntentionConfig): CustomIntention {
            return CustomIntention(intentionConfig)
        }
    }
}