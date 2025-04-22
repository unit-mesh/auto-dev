package com.phodal.shirecore.variable.frontend

import cc.unitmesh.devti.language.ast.shireql.variable.frontend.Component

interface ComponentProvider {
    fun getPages(): List<Component>
    fun getComponents(): List<Component>
    fun getRoutes(): Map<String, String>
}
