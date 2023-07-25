package cc.unitmesh.ide.webstorm.provider

import cc.unitmesh.devti.prompting.code.TestStack
import cc.unitmesh.devti.provider.TechStackProvider
import com.intellij.openapi.project.ProjectManager
import java.io.File

class JavaScriptTechStackService : TechStackProvider() {
    override fun prepareLibrary(): TestStack {
        val project = ProjectManager.getInstance().openProjects.firstOrNull() ?: return TestStack()
        // lookup package.json in project root
        val packageJson = project.basePath?.let {
            File("$it/package.json")
        } ?: return TestStack()

        return TestStack(mutableMapOf(), mutableMapOf())
    }
}
