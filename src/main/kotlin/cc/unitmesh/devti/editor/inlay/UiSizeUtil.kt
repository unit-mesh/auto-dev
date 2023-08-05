// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package cc.unitmesh.devti.editor.inlay

import java.awt.Dimension
import javax.swing.JComponent
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty

var JComponent.preferredWidth: Int by dimensionProperty(JComponent::getPreferredSize, JComponent::setPreferredSize, Dimension::width)
var JComponent.preferredHeight: Int by dimensionProperty(JComponent::getPreferredSize, JComponent::setPreferredSize, Dimension::height)
var JComponent.maximumWidth: Int by dimensionProperty(JComponent::getMaximumSize, JComponent::setMaximumSize, Dimension::width)
var JComponent.maximumHeight: Int by dimensionProperty(JComponent::getMaximumSize, JComponent::setMaximumSize, Dimension::height)

private fun <Receiver> dimensionProperty(
    getSize: Receiver.() -> Dimension,
    setSize: Receiver.(Dimension) -> Unit,
    dimensionProperty: KMutableProperty1<Dimension, Int>
): ReadWriteProperty<Receiver, Int> {
    return object : ReadWriteProperty<Receiver, Int> {

        override fun getValue(thisRef: Receiver, property: KProperty<*>): Int {
            return dimensionProperty.get(getSize(thisRef))
        }

        override fun setValue(thisRef: Receiver, property: KProperty<*>, value: Int) {
            val size = Dimension(getSize(thisRef))
            dimensionProperty.set(size, value)
            setSize(thisRef, size)
        }
    }
}
