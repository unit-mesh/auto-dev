package cc.unitmesh.devti.prompting.code

import cc.unitmesh.devti.context.model.DtClass

data class TargetEndpoint(
    val endpoint: String,
    var controller: DtClass,
    val isNeedToCreated: Boolean = true,
)