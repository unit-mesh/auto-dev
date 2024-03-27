package cc.unitmesh.android.provider

import cc.unitmesh.devti.context.ClassContext
import cc.unitmesh.devti.provider.AutoTestService
import cc.unitmesh.devti.provider.context.TestFileContext
import com.android.tools.idea.run.AndroidRunConfiguration
import com.intellij.execution.configurations.RunProfile
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.android.util.AndroidUtils
import org.jetbrains.kotlin.psi.KtFile

class AndroidTestService : AutoTestService() {
    override fun isApplicable(element: PsiElement): Boolean = AndroidUtils.hasAndroidFacets(element.project)
    override fun runConfigurationClass(project: Project): Class<out RunProfile> = AndroidRunConfiguration::class.java

    override fun findOrCreateTestFile(sourceFile: PsiFile, project: Project, element: PsiElement): TestFileContext? {
        TODO("Not yet implemented")
    }

    override fun lookupRelevantClass(project: Project, element: PsiElement): List<ClassContext> {
        TODO("Not yet implemented")
    }
}
