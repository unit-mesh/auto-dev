package cc.unitmesh.devti.language.ast.action

import cc.unitmesh.devti.language.ast.FrontMatterType
import cc.unitmesh.devti.language.ast.MethodCall

open class DirectAction(open val processors: List<PatternActionFunc>) {
    companion object {
        fun from(fmt: FrontMatterType): DirectAction? {
            return when (fmt) {
                is FrontMatterType.ARRAY -> {
                    val list = fmt.value as List<FrontMatterType>
                    val actions = list.mapNotNull {
                        when (it) {
                            is FrontMatterType.EXPRESSION -> {
                                val methodCall = it.value as? MethodCall ?: return@mapNotNull null
                                val methodName = methodCall.objectName.display()

                                val methodArgs: List<String> = methodCall.arguments?.map { arg ->
                                    arg.toString()
                                } ?: emptyList()

                                PatternActionFunc.from(methodName, methodArgs)
                            }

                            else -> {
                                null
                            }
                        }
                    }

                    return DirectAction(actions)
                }

                else -> {
                    null
                }
            }
        }
    }
}