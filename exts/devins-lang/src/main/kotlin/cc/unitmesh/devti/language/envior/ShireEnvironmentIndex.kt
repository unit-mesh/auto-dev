package cc.unitmesh.devti.language.envior

import com.intellij.json.psi.*
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.indexing.*
import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.EnumeratorStringDescriptor
import com.intellij.util.io.KeyDescriptor


class ShireEnvironmentIndex : FileBasedIndexExtension<String, Set<String>>() {
    companion object {
        val SHIRE_ENV_ID: ID<String, Set<String>> = ID.create("shire.environment")
        const val MODEL_TITLE = "title"
        const val MODEL_LIST = "models"

        fun id(): ID<String, Set<String>> {
            return SHIRE_ENV_ID
        }
    }

    override fun getValueExternalizer(): DataExternalizer<Set<String>> = ShireStringsExternalizer()
    override fun getVersion(): Int = 2
    override fun getInputFilter(): FileBasedIndex.InputFilter = ShireEnvironmentInputFilter()
    override fun dependsOnFileContent(): Boolean = true
    override fun getName(): ID<String, Set<String>> = SHIRE_ENV_ID
    override fun getKeyDescriptor(): KeyDescriptor<String> = EnumeratorStringDescriptor.INSTANCE

    override fun getIndexer(): DataIndexer<String, Set<String>, FileContent> {
        return DataIndexer { inputData: FileContent ->
            val file = inputData.psiFile
            require(file is JsonFile) { AssertionError() }

            val variablesFromFile = getVariablesFromFile(file)
            variablesFromFile
        }
    }

    private fun getVariablesFromFile(file: JsonFile): Map<String, Set<String>> {
        val result: MutableMap<String, Set<String>> = HashMap()
        when (val topLevelValue = file.topLevelValue) {
            is JsonObject -> {
                for (property in topLevelValue.propertyList) {
                    when (val value = property.value) {
                        is JsonObject -> {
                            result[property.name] = readEnvVariables(value, file.name)
                        }

                        is JsonArray -> {
                            // the prop key should be "models"
                            if (property.name != MODEL_LIST) {
                                continue
                            }

                            // the child elements of the array are objects, which should have prop call "name"
                            val envVariables = value.children
                                .filterIsInstance<JsonObject>()
                                .mapNotNull { obj ->
                                    val name = obj.findProperty(MODEL_TITLE)?.value?.text
                                    name?.let { StringUtil.unquoteString(it) }
                                }
                                .toSet()

                            result[property.name] = envVariables
                        }
                    }
                }
            }
        }

        return result
    }

    private fun readEnvVariables(obj: JsonObject, fileName: String): Set<String> {
        val properties = obj.propertyList
        return if (properties.isEmpty()) {
            emptySet()
        } else {
            val set = properties.stream()
                .map { property ->
                    StringUtil.nullize(property.name)
                }
                .toList()
                .mapNotNull { it }
                .toSet()

            set
        }
    }
}
