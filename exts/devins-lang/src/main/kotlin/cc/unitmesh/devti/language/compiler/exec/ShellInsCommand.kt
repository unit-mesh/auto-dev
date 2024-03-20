package cc.unitmesh.devti.language.compiler.exec

import cc.unitmesh.devti.language.utils.lookupFile
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.sh.run.ShRunner

class ShellInsCommand(val myProject: Project, val prop: String) : InsCommand {
    override fun execute(): String? {
        val virtualFile = myProject.lookupFile(prop.trim()) ?: return "<DevInsError>: File not found: $prop"

        val workingDirectory = virtualFile.parent.path
        val shRunner = ApplicationManager.getApplication().getService(
            ShRunner::class.java
        )
        if (shRunner != null && shRunner.isAvailable(myProject)) {
            shRunner.run(myProject, virtualFile.path, workingDirectory, "RunDevInsShell", true)
        }

        return ""
    }
}
