// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package cc.unitmesh.devti.language.parser

import cc.unitmesh.devti.language.psi.DevInTypes
import cc.unitmesh.devti.util.parser.CodeFence
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.ElementManipulators
import com.intellij.psi.LiteralTextEscaper
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.impl.source.tree.injected.InjectionBackgroundSuppressor
import com.intellij.psi.templateLanguages.OuterLanguageElement
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.*

class CodeBlockElement(node: ASTNode) : ASTWrapperPsiElement(node), PsiLanguageInjectionHost,
    InjectionBackgroundSuppressor {
    override fun isValidHost(): Boolean {
        return isAbleToAcceptInjections(this)
    }

    private fun isAbleToAcceptInjections(host: CodeBlockElement): Boolean {
        val hasStartBlock = host.firstChild?.elementType != DevInTypes.CODE_BLOCK_START
        val hasEndBlock = host.lastChild?.elementType != DevInTypes.CODE_BLOCK_END
        val hasContents = host.children.count { it.hasType(DevInTypes.CODE_CONTENTS) } < 2

        return !(hasStartBlock && hasEndBlock && hasContents)
    }

    override fun updateText(text: String): PsiLanguageInjectionHost {
        return ElementManipulators.handleContentChange(this, text)
    }

    override fun createLiteralTextEscaper(): LiteralTextEscaper<out PsiLanguageInjectionHost> {
        return CodeBlockLiteralTextEscaper(this)
    }

    fun getLanguageId(): PsiElement? {
        return findChildByType(DevInTypes.LANGUAGE_ID)
    }

    fun codeText(): String {
        return CodeFence.parse(this.text).text
    }

    companion object {
        fun obtainFenceContent(element: CodeBlockElement): List<PsiElement>? {
            return CachedValuesManager.getCachedValue(element) {
                CachedValueProvider.Result.create(getContent(element), element)
            }
        }

        private fun getContent(host: CodeBlockElement): List<PsiElement>? {
            val children = host.firstChild
                ?.siblings(forward = true, withSelf = true) ?: return null

            val elements =
                children.filter {
                    it !is OuterLanguageElement
                            && (it.node.elementType == DevInTypes.CODE_CONTENTS || it == DevInTypes.NEWLINE)
                }
                    .toList()

            if (elements.isNotEmpty() && elements.first() == DevInTypes.NEWLINE) {
                elements.drop(1)
            }
            if (elements.isNotEmpty() && elements.last() == DevInTypes.NEWLINE) {
                elements.dropLast(1)
            }

            return elements.takeIf { it.isNotEmpty() }
        }

        fun obtainRelevantTextRange(element: CodeBlockElement): TextRange {
            val elements = obtainFenceContent(element) ?: return getEmptyRange(element)
            val first = elements.first()
            val last = elements.last()

            return TextRange.create(first.startOffsetInParent, last.startOffsetInParent + last.textLength)
        }

        private fun getEmptyRange(host: CodeBlockElement): TextRange {
            val start = host.children.find { it.hasType(DevInTypes.LANGUAGE_ID) }
                ?: host.children.find { it.hasType(DevInTypes.CODE_BLOCK_START) }

            return TextRange.from(start!!.startOffsetInParent + start.textLength + 1, 0)
        }
    }
}

fun PsiElement.hasType(type: IElementType): Boolean {
    return PsiUtilCore.getElementType(this) == type
}

