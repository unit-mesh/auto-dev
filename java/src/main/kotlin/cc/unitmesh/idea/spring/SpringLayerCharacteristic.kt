package cc.unitmesh.idea.spring

import com.intellij.psi.PsiClass
import kotlinx.serialization.Serializable

@Serializable
class SpringLayerCharacteristic(val annotation: String, val imports: List<String>, val codeRegex: String, val fileName: String? = null) {
    companion object {
        private val controllerCharacteristic = SpringLayerCharacteristic(
            annotation = "@Controller",
            imports = listOf(
                "org.springframework.stereotype.Controller",
                "org.springframework.web.bind.annotation.RestController"
            ),
            codeRegex = "public\\s+class\\s+\\w+Controller",
            fileName = ".*Controller\\.java"
        )

        private val serviceCharacteristic = SpringLayerCharacteristic(
            annotation = "@Service",
            imports = listOf("org.springframework.stereotype.Service"),
            codeRegex = "public\\s+(class|interface)\\s+\\w+Service",
            fileName = ".*(Service|ServiceImpl)\\.java"
        )

        private val entityCharacteristic = SpringLayerCharacteristic(
            annotation = "@Entity",
            imports = listOf("javax.persistence.Entity"),
            codeRegex = "public\\s+class\\s+\\w+Entity",
        )

        private val dtoCharacteristic = SpringLayerCharacteristic(
            annotation = "@Data",
            imports = listOf("lombok.Data"),
            codeRegex = "public\\s+class\\s+\\w+(Dto|DTO|Request|Response|Res|Req)",
            fileName = ".*(Dto|DTO|Request|Response|Res|Req)\\.java"
        )

        private val repositoryCharacteristic = SpringLayerCharacteristic(
            annotation = "org.springframework.stereotype.Repository",
            imports = listOf("org.springframework.stereotype.Repository"),
            codeRegex = "public\\s+(class|interface)\\s+\\w+Repository",
            fileName = ".*Repository\\.java"
        )

        private val allCharacteristics = mapOf(
            "controller" to controllerCharacteristic,
            "service" to serviceCharacteristic,
            "entity" to entityCharacteristic,
            "dto" to dtoCharacteristic,
            "repository" to repositoryCharacteristic
        )


        fun check(code: String, type: String): Boolean {
            val characteristic = allCharacteristics[type] ?: return false
            if (code.contains(characteristic.annotation)) {
                return true
            }

            characteristic.imports.forEach {
                if (code.contains(it)) {
                    return true
                }
            }

            val regex = Regex(characteristic.codeRegex)
            return regex.containsMatchIn(code)
        }

        fun check(code: PsiClass, type: String): Boolean {
            val characteristic = allCharacteristics[type] ?: return false
            code.annotations.forEach {
                if (characteristic.imports.contains(it.qualifiedName)) {
                    return true
                }
            }

            if (characteristic.fileName != null) {
                val regex = Regex(characteristic.fileName)
                code.name?.lowercase()?.let {
                    if (regex.containsMatchIn(it)) {
                        return true
                    }
                }
            }

            val regex = Regex(characteristic.codeRegex)
            return regex.containsMatchIn(code.text)
        }
    }
}