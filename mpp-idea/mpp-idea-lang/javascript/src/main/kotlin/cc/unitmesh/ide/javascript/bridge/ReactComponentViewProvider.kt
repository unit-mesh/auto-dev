package cc.unitmesh.ide.javascript.bridge

import cc.unitmesh.devti.bridge.archview.model.UiComponent
import cc.unitmesh.devti.bridge.provider.ComponentViewMode
import cc.unitmesh.ide.javascript.flow.ReactAutoPage
import cc.unitmesh.ide.javascript.util.ReactPsiUtil
import com.intellij.lang.javascript.JavaScriptFileType
import com.intellij.lang.javascript.TypeScriptJSXFileType
import com.intellij.lang.javascript.dialects.ECMA6LanguageDialect
import com.intellij.lang.javascript.dialects.TypeScriptJSXLanguageDialect
import com.intellij.lang.javascript.psi.JSFile
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import kotlin.collections.plusAssign

class ReactComponentViewProvider : FrameworkComponentViewProvider("React") {
    override fun collect(project: Project, mode: ComponentViewMode): List<UiComponent> {
        val searchScope: GlobalSearchScope = ProjectScope.getContentScope(project)
        val psiManager = PsiManager.getInstance(project)

        val virtualFiles =
            FileTypeIndex.getFiles(JavaScriptFileType.INSTANCE, searchScope) +
                    FileTypeIndex.getFiles(TypeScriptJSXFileType.INSTANCE, searchScope)

        val components = mutableListOf<UiComponent>()
        virtualFiles.forEach { file ->
            val jsFile = (psiManager.findFile(file) ?: return@forEach) as? JSFile ?: return@forEach
            if (jsFile.isTestFile) return@forEach

            components += buildComponent(jsFile) ?: return@forEach
        }

        return components
    }

    companion object {
        fun buildComponent(jsFile: JSFile): List<UiComponent>? {
            return when (jsFile.language) {
                is TypeScriptJSXLanguageDialect,
                is ECMA6LanguageDialect
                    -> {
                    val dsComponents = ReactPsiUtil.tsxComponentToComponent(jsFile)
                    if (dsComponents.isEmpty()) {
                        logger<ReactAutoPage>().warn("no component found in ${jsFile.name}")
                    }
                    dsComponents
                }

                else -> {
                    logger<ReactAutoPage>().warn("unknown language: ${jsFile.language}")
                    null
                }
            }
        }

    }
}