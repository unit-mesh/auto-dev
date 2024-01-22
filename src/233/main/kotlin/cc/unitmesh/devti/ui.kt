package cc.unitmesh.devti

import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.Row
import javax.swing.JComponent

fun <T : JComponent> Row.fullWidthCell(component: T): Cell<T> {
    return cell(component)
        .align(Align.FILL)
}

fun <T : JComponent> Cell<T>.fullWidth(): Cell<T> {
    return this.align(Align.FILL)
}