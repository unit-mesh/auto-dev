package cc.unitmesh.devti.language.envior.llm

import com.intellij.json.psi.JsonArray
import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonObject
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.FileBasedIndex
import cc.unitmesh.devti.language.envior.ShireEnvironmentIndex
import cc.unitmesh.devti.language.envior.valueAsString

class LlmEnv {
    companion object {
        private fun configFromFile(modelName: String, psiFile: JsonFile?): JsonObject? {
            val rootObject = psiFile?.topLevelValue as? JsonObject ?: return null
            val envObject = rootObject.propertyList.firstOrNull { it.name == ShireEnvironmentIndex.MODEL_LIST }?.value as? JsonArray
            return envObject?.children?.firstOrNull {
                it is JsonObject && it.findProperty(ShireEnvironmentIndex.MODEL_TITLE)?.valueAsString(it) == modelName
            } as? JsonObject
        }

        fun configFromFile(modelName: String, scope: GlobalSearchScope, project: Project): JsonObject? {
            val jsonFile = runReadAction {
                FileBasedIndex.getInstance().getContainingFiles(ShireEnvironmentIndex.id(), ShireEnvironmentIndex.MODEL_LIST, scope)
                    .firstOrNull()
                    ?.let {
                        (PsiManager.getInstance(project).findFile(it) as? JsonFile)
                    }
            }

            return configFromFile(modelName, jsonFile)
        }
    }
}