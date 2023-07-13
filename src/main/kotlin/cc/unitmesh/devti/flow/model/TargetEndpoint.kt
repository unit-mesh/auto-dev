package cc.unitmesh.devti.flow.model

import cc.unitmesh.devti.analysis.DtClass

data class TargetEndpoint(
    val endpoint: String,
    var controller: DtClass,
    val isNeedToCreated: Boolean = true,
)