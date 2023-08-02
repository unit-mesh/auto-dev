package cc.unitmesh.devti.gui.block

import java.awt.Component

interface MessageBlockView {
    fun getBlock(): MessageBlock

    fun getComponent(): Component?

    fun initialize() {}
}