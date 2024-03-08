package cc.unitmesh.devti.counit.view

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.temporary.gui.block.*
import java.awt.Component


class WebBlock(val msg: CompletableMessage) : AbstractMessageBlock(msg) {
    override val type: MessageBlockType = MessageBlockType.PlainText
}

class WebBlockView(
    private val block: WebBlock,
    private val project: Project,
    private val disposable: Disposable,
) : MessageBlockView {
    private val webViewWindow = WebViewWindow()

    override fun initialize() {
        this.webViewWindow.loadHtml(block.msg.text)
    }

    override fun getBlock(): MessageBlock {
        return block
    }

    override fun getComponent(): Component {
        return webViewWindow.component
    }
}