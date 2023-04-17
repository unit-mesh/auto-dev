package cc.unitmesh.devti.analysis

import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.openapi.project.Project
import java.util.*

class JavaCrudTemplate(val project: Project) {
    private val fileTemplateManager = FileTemplateManager.getInstance(project)

    fun controller(
        endpoint: String,
        code: String,
        packageName: String?
    ): String {
        val templateCode = """
                |package $packageName;
                |
                |import org.springframework.stereotype.Controller;
                |import org.springframework.web.bind.annotation.RequestMapping;
                |
                |@Controller
                |class $endpoint {
                |$code
                |}""".trimMargin()
        return templateCode
    }

    // todo: make it works
    fun templateController(
        controllerName: String,
        code: String,
        packageName: String?
    ): String {
        val template = fileTemplateManager.getTemplate("Java Controller")

        val properties = Properties()
        properties.setProperty("packageName", packageName)
        properties.setProperty("controllerName", controllerName)
        properties.setProperty("code", code)

        return template.getText(properties)
    }
}