package cc.unitmesh.devti

import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.Row
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import javax.swing.JComponent

fun <T : JComponent> Row.fullWidthCell(component: T): Cell<T> {
    return cell(component)
        .horizontalAlign(HorizontalAlign.FILL)
}

// full width cell
fun <T : JComponent> Cell<T>.fullWidth(): Cell<T> {
    return this.horizontalAlign(HorizontalAlign.FILL)
}
