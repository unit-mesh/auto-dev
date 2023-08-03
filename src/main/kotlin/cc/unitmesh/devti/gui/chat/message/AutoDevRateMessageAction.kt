package cc.unitmesh.devti.gui.chat.message

import cc.unitmesh.devti.AutoDevIcons
import cc.unitmesh.devti.gui.block.CompletableMessage
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareToggleAction
import javax.swing.Icon


enum class ChatMessageRating {
    None,
    Like,
    Dislike
}

abstract class AutoDevRateMessageAction : DumbAwareToggleAction() {
    abstract fun getReaction(): ChatMessageRating
    abstract fun getReactionIcon(): Icon
    abstract fun getReactionIconSelected(): Icon

    private fun getMessage(event: AnActionEvent): CompletableMessage? {
        return event.dataContext.getData<CompletableMessage>(CompletableMessage.key)
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        val icon: Icon = if (!isSelected(e)) getReactionIcon() else getReactionIconSelected()
        e.presentation.setIcon(icon)

        // SET HOVER TOOLTIP FOR icon
        e.presentation.description = "Like this message"
    }

    override fun isSelected(e: AnActionEvent): Boolean {
        return getMessage(e)?.rating == getReaction()
    }

    override fun setSelected(e: AnActionEvent, state: Boolean) {
        val message = getMessage(e) ?: return
        val completableMessage = getMessage(e)
        message.rating = if (isSelected(e)) ChatMessageRating.None else getReaction()
    }


    class Like : AutoDevRateMessageAction() {
        override fun getReaction(): ChatMessageRating = ChatMessageRating.Like

        override fun getReactionIcon(): Icon = AutoDevIcons.Like

        override fun getReactionIconSelected(): Icon = AutoDevIcons.Liked
    }

    class Dislike : AutoDevRateMessageAction() {
        override fun getReaction(): ChatMessageRating = ChatMessageRating.Dislike

        override fun getReactionIcon(): Icon = AutoDevIcons.Dislike

        override fun getReactionIconSelected(): Icon = AutoDevIcons.Disliked

    }
}
