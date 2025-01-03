// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.temporary.gui.block

import cc.unitmesh.devti.util.parser.CodeFence
import com.intellij.lang.Language
import java.math.BigInteger
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

interface MessageBlock {
    val type: MessageBlockType
    fun getTextContent(): String
    fun getMessage(): CompletableMessage
    fun setNeedUpdate(needUpdate: Boolean)
    fun isNeedUpdate(): Boolean
    fun getIdentifier(): String
    fun getMessageBlockView(): MessageBlockView?
    fun setMessageBlockView(messageBlockView: MessageBlockView)
    fun addContent(addedContent: String)
    fun replaceContent(content: String)
    fun addTextListener(textListener: MessageBlockTextListener)
    fun removeTextListener(textListener: MessageBlockTextListener)
}

abstract class AbstractMessageBlock(open val completableMessage: CompletableMessage) : MessageBlock {
    private val contentBuilder: StringBuilder = StringBuilder()
    private var identifier: String = ""
    private var needUpdate: Boolean = true
    private var messageBlockView: MessageBlockView? = null
    private val textListeners: MutableList<MessageBlockTextListener> = mutableListOf()

    companion object {
        fun encode(bytes: ByteArray): String {
            var md5 = ""
            try {
                val messageDigest = MessageDigest.getInstance("MD5")
                messageDigest.update(bytes)
                md5 = BigInteger(1, messageDigest.digest()).toString(16)
            } catch (noSuchAlgorithmException: NoSuchAlgorithmException) {
                // empty catch block
            }
            return md5
        }
    }
    override fun isNeedUpdate(): Boolean {
        return needUpdate
    }

    override fun setNeedUpdate(needUpdate: Boolean) {
        this.needUpdate = needUpdate
    }

    override fun getMessageBlockView(): MessageBlockView? {
        return messageBlockView;
    }

    override fun setMessageBlockView(messageBlockView: MessageBlockView) {
        this.messageBlockView = messageBlockView;
    }

    override fun getIdentifier(): String {
        return identifier;
    }

    override fun addContent(addedContent: String) {
        contentBuilder.clear()
        contentBuilder.append(addedContent)
        onContentAdded(addedContent)
        val content = contentBuilder.toString()
        identifier = encode(content.toByteArray())
        onContentChanged(content)
        fireTextChanged(content)
    }

    override fun replaceContent(content: String) {
        contentBuilder.clear()
        contentBuilder.append(content)
        identifier = encode(content.toByteArray())
        onContentChanged(content)
        fireTextChanged(content)
    }

    override fun getTextContent(): String {
        return contentBuilder.toString()
    }

    override fun getMessage(): CompletableMessage {
        return completableMessage
    }

    protected fun onContentAdded(addedContent: String) {}
    protected open fun onContentChanged(content: String) {}
    private fun fireTextChanged(text: String) {
        for (textListener in textListeners) {
            textListener.onTextChanged(text)
        }
    }

    override fun addTextListener(textListener: MessageBlockTextListener) {
        textListeners.add(textListener)
    }

    override fun removeTextListener(textListener: MessageBlockTextListener) {
        textListeners.remove(textListener)
    }
}

class TextBlock(val msg: CompletableMessage) : AbstractMessageBlock(msg) {
    override val type: MessageBlockType = MessageBlockType.PlainText
}

class CodeBlock(private val msg: CompletableMessage, language: Language = Language.ANY) : AbstractMessageBlock(msg) {
    override var type: MessageBlockType = MessageBlockType.CodeEditor

    var code: CodeFence

    init {
        this.code = CodeFence(language, msg.text, false)
    }

    override fun onContentChanged(content: String) {
        this.code = CodeFence.parse(content)
    }
}