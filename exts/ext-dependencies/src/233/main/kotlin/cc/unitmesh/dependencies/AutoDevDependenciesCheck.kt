package cc.unitmesh.dependencies

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.AutoDevNotifications
import cc.unitmesh.devti.provider.BuildSystemProvider
import cc.unitmesh.devti.sketch.lint.SketchCodeInspection
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.packageChecker.api.BuildFileProvider
import com.intellij.psi.PsiManager
import org.jetbrains.security.`package`.Package
import org.jetbrains.security.`package`.PackageType

class AutoDevDependenciesCheck : AnAction(AutoDevBundle.message("sketch.dependencies.check")) {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

    override fun update(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(PlatformDataKeys.EDITOR) ?: return
        val document = editor.document
        val file = FileDocumentManager.getInstance().getFile(document) ?: return

        if (BuildSystemProvider.isDeclarePackageFile(file.name)) {
            e.presentation.isVisible = false
            return
        }

        val psiFile = PsiManager.getInstance(project).findFile(file) ?: return
        e.presentation.isVisible = BuildFileProvider.EP_NAME.getExtensions(project).any {
            it.supports(psiFile)
        }
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val editor = event.getData(PlatformDataKeys.EDITOR) ?: return
        val document = editor.document
        val file = FileDocumentManager.getInstance().getFile(document) ?: return
        val psiFile = PsiManager.getInstance(project).findFile(file) ?: return

        val dependencies: List<Package> = BuildSystemProvider.EP_NAME.extensionList.map {
            it.collectDependencies(project, psiFile)
        }.flatten().map {
            Package(PackageType.fromString(it.type), it.namespace, it.name, it.version, it.qualifiers, it.subpath)
        }

        val runInspections = SketchCodeInspection.runInspections(
            project,
            psiFile,
            psiFile.virtualFile,
            HighlightSeverity.WARNING
        )

        AutoDevNotifications.notify(
            project,
            "Found ${dependencies.size} dependencies" + " Dependencies check: ${runInspections.size} issues found"
        )
    }
}
