package cc.unitmesh.devti

import com.intellij.ui.dsl.builder.*
import javax.swing.JComponent

fun <T : JComponent> Row.fullWidthCell(component: T): Cell<T> {
    return cell(component)
        .align(Align.FILL)
}

fun <T : JComponent> Cell<T>.fullWidth(): Cell<T> {
    return this.align(AlignX.FILL)
}

fun <T : JComponent> Cell<T>.fullHeight(): Cell<T> {
    return this.align(AlignY.FILL)
}

// align right
fun <T : JComponent> Cell<T>.alignRight(): Cell<T> {
    return this.align(AlignX.RIGHT)
}