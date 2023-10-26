package com.intellij.temporary.inlay

import com.intellij.openapi.editor.Inlay
import com.intellij.util.ui.JBUI
import javax.swing.JComponent
import javax.swing.JPanel

open class InlayPanel<T : JComponent?>(var component: T) : JPanel() {
    protected open fun setupPane(inlay: Inlay<*>) {
        add(this.component)
        setOpaque(false)
        setBorder(JBUI.Borders.empty())
        setLayout(InlayLayoutManager(inlay))
    }
}