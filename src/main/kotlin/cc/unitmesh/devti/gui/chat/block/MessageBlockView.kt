package cc.unitmesh.devti.gui.chat.block

import java.awt.Component

interface MessageBlockView {
    fun getBlock(): MessageBlock

    fun getComponent(): Component?

    fun initialize() {}
}