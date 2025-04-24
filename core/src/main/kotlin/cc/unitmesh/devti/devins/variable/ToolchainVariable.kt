package cc.unitmesh.devti.devins.variable

import org.reflections.Reflections
import kotlin.jvm.kotlin
import kotlin.reflect.KClass
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.functions

interface ToolchainVariable : Variable {
    companion object {
        private val subclasses: Set<KClass<out ToolchainVariable>> by lazy {
            val reflections = Reflections("cc.unitmesh.devti.devins.variable")
            reflections.getSubTypesOf(ToolchainVariable::class.java)
                .map { it.kotlin }
                .toSet()
        }

        fun from(variableName: String): ToolchainVariable? {
            for (subclass in subclasses) {
                val companion = subclass.companionObjectInstance ?: continue
                val fromFunction = companion::class.declaredFunctions.find { it.name == "from" } ?: continue
                val result = fromFunction.call(companion, variableName) as? ToolchainVariable
                if (result != null) {
                    return result
                }
            }
            return null
        }

        fun all(): List<ToolchainVariable> {
            val allVariables = mutableListOf<ToolchainVariable>()
            for (subclass in subclasses) {
                val valuesFunction = subclass.functions.find { it.name == "values" } ?: continue
                val enumConstants = valuesFunction.call() as Array<ToolchainVariable>
                allVariables.addAll(enumConstants)
            }

            return allVariables
        }
    }
}