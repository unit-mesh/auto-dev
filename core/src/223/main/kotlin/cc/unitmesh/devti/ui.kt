package cc.unitmesh.devti

import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.Row
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.ui.dsl.gridLayout.VerticalAlign
import javax.swing.JComponent

fun <T : JComponent> Row.fullWidthCell(component: T): Cell<T> {
    return cell(component)
        .horizontalAlign(HorizontalAlign.FILL)
}

// full width cell
fun <T : JComponent> Cell<T>.fullWidth(): Cell<T> {
    return this.horizontalAlign(HorizontalAlign.FILL)
}

// full height and height
fun <T : JComponent> Cell<T>.fullHeight(): Cell<T> {
    return this.verticalAlign(VerticalAlign.FILL)
}

// align right
fun <T : JComponent> Cell<T>.alignRight(): Cell<T> {
    return this.horizontalAlign(HorizontalAlign.RIGHT)
}
