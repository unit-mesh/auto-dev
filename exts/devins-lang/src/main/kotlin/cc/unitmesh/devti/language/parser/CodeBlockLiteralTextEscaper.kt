// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package cc.unitmesh.devti.language.parser

import com.intellij.openapi.util.TextRange
import com.intellij.psi.LiteralTextEscaper

class CodeBlockLiteralTextEscaper(host: CodeBlockElement) : LiteralTextEscaper<CodeBlockElement>(host) {
    override fun getRelevantTextRange() = CodeBlockElement.obtainRelevantTextRange(myHost)
    override fun isOneLine(): Boolean = false;

    override fun decode(rangeInsideHost: TextRange, outChars: StringBuilder): Boolean {
        val elements = CodeBlockElement.obtainFenceContent(myHost) ?: return true
        for (element in elements) {
            val intersected = rangeInsideHost.intersection(element.textRangeInParent) ?: continue
            outChars.append(intersected.substring(myHost.text))
        }

        return true
    }

    override fun getOffsetInHost(offsetInDecoded: Int, rangeInsideHost: TextRange): Int {
        val elements = CodeBlockElement.obtainFenceContent(myHost) ?: return -1
        var cur = 0
        for (element in elements) {
            val intersected = rangeInsideHost.intersection(element.textRangeInParent)
            if (intersected == null || intersected.isEmpty) continue
            if (cur + intersected.length == offsetInDecoded) {
                return intersected.startOffset + intersected.length
            } else if (cur == offsetInDecoded) {
                return intersected.startOffset
            } else if (cur < offsetInDecoded && cur + intersected.length > offsetInDecoded) {
                return intersected.startOffset + (offsetInDecoded - cur)
            }
            cur += intersected.length
        }

        val last = elements[elements.size - 1]
        val intersected = rangeInsideHost.intersection(last.textRangeInParent)
        if (intersected == null || intersected.isEmpty) return -1
        val result = intersected.startOffset + (offsetInDecoded - (cur - intersected.length))
        return if (rangeInsideHost.startOffset <= result && result <= rangeInsideHost.endOffset) {
            result
        } else -1
    }
}