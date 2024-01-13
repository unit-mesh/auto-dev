package cc.unitmesh.devti

import com.intellij.openapi.util.IconLoader
import com.intellij.ui.AnimatedIcon
import javax.swing.Icon

object AutoDevIcons {
    @JvmField
    val STORY: Icon = IconLoader.getIcon("/icons/story.svg", AutoDevIcons::class.java)

    @JvmField
    val AI_COPILOT: Icon = IconLoader.getIcon("/icons/ai-copilot.svg", AutoDevIcons::class.java)

    @JvmField
    val DARK: Icon = IconLoader.getIcon("/icons/autodev-dark.svg", AutoDevIcons::class.java)

    @JvmField
    val ERROR: Icon = IconLoader.getIcon("/icons/autodev-error.svg", AutoDevIcons::class.java)

    @JvmField
    val IntProgress = AnimatedIcon.Default()

    @JvmField
    val Send: Icon = IconLoader.getIcon("/icons/send.svg", AutoDevIcons::class.java)

    @JvmField
    val Like: Icon = IconLoader.getIcon("/icons/like.svg", AutoDevIcons::class.java)

    @JvmField
    val Liked: Icon = IconLoader.getIcon("/icons/liked.svg", AutoDevIcons::class.java)

    @JvmField
    val Dislike: Icon = IconLoader.getIcon("/icons/dislike.svg", AutoDevIcons::class.java)

    @JvmField
    val Disliked: Icon = IconLoader.getIcon("/icons/disliked.svg", AutoDevIcons::class.java)
}
