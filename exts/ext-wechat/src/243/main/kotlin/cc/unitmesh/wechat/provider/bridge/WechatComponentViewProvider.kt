package cc.unitmesh.wechat.provider.bridge

import cc.unitmesh.devti.bridge.archview.model.UiComponent
import cc.unitmesh.devti.bridge.provider.ComponentViewMode
import cc.unitmesh.devti.bridge.provider.ComponentViewProvider
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import com.intellij.wechat.miniprogram.lang.wxml.WxmlFileType
import com.intellij.wechat.miniprogram.lang.wxml.psi.impl.WxmlFile
import com.intellij.wechat.miniprogram.project.WxnProjectManager

class WechatComponentViewProvider : ComponentViewProvider() {
    override fun isApplicable(project: Project): Boolean {
        return WxnProjectManager.getInstance(project).isWxnProject()
    }

    override fun collect(
        project: Project,
        mode: ComponentViewMode
    ): List<UiComponent> {
        val searchScope: GlobalSearchScope = ProjectScope.getContentScope(project)
        val psiManager = PsiManager.getInstance(project)

        val virtualFiles = FileTypeIndex.getFiles(WxmlFileType, searchScope)

        val components = mutableListOf<UiComponent>()
        virtualFiles.forEach { file ->
            val wxmlFile = (psiManager.findFile(file) ?: return@forEach) as? WxmlFile ?: return@forEach
            components += buildComponent(wxmlFile) ?: return@forEach
        }

        return components
    }

    companion object {
        fun buildComponent(jsFile: WxmlFile): List<UiComponent>? {
            return emptyList()
        }
    }
}
