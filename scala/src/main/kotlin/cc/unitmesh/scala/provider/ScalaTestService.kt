package cc.unitmesh.scala.provider

import cc.unitmesh.devti.context.ClassContext
import cc.unitmesh.devti.provider.AutoTestService
import cc.unitmesh.devti.provider.context.TestFileContext
import com.intellij.execution.RunManager
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunProfile
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile
import org.jetbrains.plugins.scala.lang.psi.api.ScalaPsiElement
import org.jetbrains.plugins.scala.testingSupport.test.scalatest.ScalaTestConfigurationType
import org.jetbrains.plugins.scala.testingSupport.test.scalatest.ScalaTestRunConfiguration
import org.jetbrains.plugins.scala.testingSupport.test.testdata.AllInPackageTestData

class ScalaTestService : AutoTestService() {
    override fun isApplicable(element: PsiElement): Boolean = element is ScalaPsiElement
    override fun runConfigurationClass(project: Project): Class<out RunProfile>? = ScalaTestRunConfiguration::class.java
    override fun psiFileClass(project: Project): Class<out PsiElement> = ScalaFile::class.java

    override fun createConfiguration(project: Project, virtualFile: VirtualFile): RunConfiguration? {
        val scalaFile = PsiManager.getInstance(project).findFile(virtualFile) as? ScalaFile ?: return null
        val settings = RunManager.getInstance(project).createConfiguration("Scala tests", ScalaTestConfigurationType().confFactory())
        val configuration = settings.configuration as ScalaTestRunConfiguration
        val packageTestData = AllInPackageTestData(configuration)
        packageTestData.workingDirectory = scalaFile.project.basePath
        configuration.`testConfigurationData_$eq`(packageTestData)
        configuration.testKind = packageTestData.kind
        configuration.module = ModuleUtilCore.findModuleForPsiElement(scalaFile)

        return settings.configuration
    }

    override fun findOrCreateTestFile(sourceFile: PsiFile, project: Project, element: PsiElement): TestFileContext? {
        TODO("Not yet implemented")
    }

    override fun lookupRelevantClass(project: Project, element: PsiElement): List<ClassContext> {
        TODO("Not yet implemented")
    }

}
