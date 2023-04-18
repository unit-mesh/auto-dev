package cc.unitmesh.devti

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

class DevtiIcons {
    companion object {
        val STORY: Icon = IconLoader.getIcon("/icons/story.svg", DevtiIcons::class.java)
        val FIND_BUG: Icon = IconLoader.getIcon("/icons/find-bug.svg", DevtiIcons::class.java)
        val AI_COPILOT: Icon = IconLoader.getIcon("/icons/ai-copilot.svg", DevtiIcons::class.java)
        val COMMENTS: Icon = IconLoader.getIcon("/icons/comments.svg", DevtiIcons::class.java)
    }
}
