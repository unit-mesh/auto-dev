package cc.unitmesh.devti

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

class AutoDevIcons {
    companion object {
        val STORY: Icon = IconLoader.getIcon("/icons/story.svg", AutoDevIcons::class.java)
        val FIND_BUG: Icon = IconLoader.getIcon("/icons/find-bug.svg", AutoDevIcons::class.java)
        val AI_COPILOT: Icon = IconLoader.getIcon("/icons/ai-copilot.svg", AutoDevIcons::class.java)
        val COMMENTS: Icon = IconLoader.getIcon("/icons/comments.svg", AutoDevIcons::class.java)
    }
}
