package cc.unitmesh.devti.provider.context

import kotlin.reflect.KClass

class ChatContextItem(
    val clazz: KClass<*>,
    var text: String
)