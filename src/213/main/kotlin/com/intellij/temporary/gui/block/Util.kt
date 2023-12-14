package com.intellij.temporary.gui.block

import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.openapi.observable.properties.GraphPropertyImpl
import com.intellij.openapi.observable.properties.PropertyGraph

fun <T> PropertyGraph.property(initial: T): GraphProperty<T> {
    return GraphPropertyImpl(this) { initial }
}
