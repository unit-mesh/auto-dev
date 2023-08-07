package cc.unitmesh.devti.gui.error

import com.intellij.temporary.gui.block.AutoDevSnippetFile.isSnippet
import com.intellij.codeInsight.daemon.impl.analysis.DefaultHighlightingSettingProvider
import com.intellij.codeInsight.daemon.impl.analysis.FileHighlightingSetting
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class CodeBlockHighlightingSettingsProvider : DefaultHighlightingSettingProvider() {
    override fun getDefaultSetting(project: Project, file: VirtualFile): FileHighlightingSetting? {
        return if (isSnippet(file)) FileHighlightingSetting.SKIP_HIGHLIGHTING else null
    }
}
