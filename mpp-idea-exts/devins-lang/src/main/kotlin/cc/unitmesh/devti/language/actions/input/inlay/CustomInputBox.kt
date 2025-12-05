package cc.unitmesh.devti.language.actions.input.inlay

import com.intellij.ide.KeyboardAwareFocusOwner
import com.intellij.ui.scale.JBUIScale
import java.awt.Dimension
import java.awt.event.KeyEvent
import javax.swing.JComponent
import javax.swing.JTextField
import javax.swing.KeyStroke
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty

class CustomInputBox : JTextField(), KeyboardAwareFocusOwner {
    override fun skipKeyEventDispatcher(event: KeyEvent): Boolean = true

    init {
        this.minimumWidth = JBUIScale.scale(480)
        this.preferredSize = this.minimumSize

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), CUSTOM_INPUT_CANCEL_ACTION)
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), CUSTOM_INPUT_SUBMIT_ACTION)
    }

    companion object {
        const val CUSTOM_INPUT_CANCEL_ACTION = "custom.input.cancel"
        const val CUSTOM_INPUT_SUBMIT_ACTION = "custom.input.submit"
    }
}

var JComponent.minimumWidth: Int by dimensionProperty(JComponent::getMinimumSize, JComponent::setMinimumSize, Dimension::width)

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

