package cc.unitmesh.idea

import com.intellij.openapi.module.LanguageLevelUtil
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.JavaSdkType
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.pom.java.LanguageLevel
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiUtil

fun detectLanguageLevel(project: Project, sourceFile: PsiFile?): LanguageLevel? {
    val projectSdk = ProjectRootManager.getInstance(project).projectSdk
    if (projectSdk != null) {
        if (projectSdk.sdkType !is JavaSdkType) return null
        return PsiUtil.getLanguageLevel(project)
    }

    var moduleForFile = ModuleUtilCore.findModuleForFile(sourceFile)
    if (moduleForFile == null) {
        moduleForFile = ModuleManager.getInstance(project).modules.firstOrNull() ?: return null
    }

    val sdk = ModuleRootManager.getInstance(moduleForFile).sdk ?: return null
    if (sdk.sdkType !is JavaSdkType) return null

    return LanguageLevelUtil.getEffectiveLanguageLevel(moduleForFile)
}