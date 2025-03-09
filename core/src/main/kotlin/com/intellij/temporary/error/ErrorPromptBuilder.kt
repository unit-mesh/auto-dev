// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.temporary.error

import com.intellij.temporary.error.ErrorPlacePsiUtils
import cc.unitmesh.devti.llms.tokenizer.Tokenizer
import cc.unitmesh.devti.prompting.TextTemplatePrompt
import cc.unitmesh.devti.template.GENIUS_ERROR
import cc.unitmesh.devti.template.TemplateRender
import cc.unitmesh.devti.template.context.TemplateContext
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import java.lang.String.format
import kotlin.math.min

data class ErrorContext(
    val errorText: String,
    val sourceCode: String,
) : TemplateContext

class ErrorPromptBuilder(private val maxLength: Int, private val tokenizer: Tokenizer) {
    private val displayText =
        "Please help me understand what the problem is and try to fix the code. Here's the console output and the program text:\nConsole output:\n%s\nTexts of programs:\n%s"

    @JvmSynthetic
    fun buildPrompt(errorText: String, list: List<ErrorPlace>): TextTemplatePrompt {
        var sourceCode = ""
        val requestLength = buildDisplayPrompt("", sourceCode, "").requestText.length ?: 0
        val maxLengthForPiece = (maxLength - requestLength) / 2
        var currentMaxTokenCount = maxLengthForPiece
        val listOfIncludedDiapasons = mutableListOf<ErrorScope>()

        list.forEach { errorPlace ->
            if (!errorPlace.isProjectFile) return@forEach

            val isLineIncluded =
                listOfIncludedDiapasons.any { it.containsLineNumber(errorPlace.lineNumber, errorPlace.virtualFile) }

            if (!isLineIncluded) {
                val trimmed = trimByGreedyScopeSelection(errorPlace, currentMaxTokenCount) ?: return@forEach

                currentMaxTokenCount -= tokenizer.count(trimmed.text)
                sourceCode += trimmed.text
                listOfIncludedDiapasons.add(trimmed)
            }
        }

        val errorTextTrimmed = trimTextByTokenizer(errorText, maxLengthForPiece)
        val formattedDisplayText = format(displayText, "```\n$errorTextTrimmed\n```\n", sourceCode)

        return buildDisplayPrompt(errorTextTrimmed, sourceCode, formattedDisplayText)
    }

    private fun trimByGreedyScopeSelection(errorPlace: ErrorPlace, maxTokenCount: Int): ErrorScope? {
        return ReadAction.compute<ErrorScope?, Throwable> {
            val language: String = errorPlace.getMarkDownLanguageSlug() ?: ""

            tryFitAllFile(
                errorPlace.hyperlinkText,
                errorPlace.programText,
                maxTokenCount,
                language,
                errorPlace.virtualFile
            ) ?: findEnclosingScopeGreedy(errorPlace, maxTokenCount, language)
        }
    }

    private fun findEnclosingScopeGreedy(errorPlace: ErrorPlace, maxTokenCount: Int, language: String): ErrorScope? {
        var result: ErrorScope? = null
        var containingElement = errorPlace.findContainingElement()

        while (true) {
            if (containingElement is PsiFile || containingElement == null) break

            result = tryFitContainingElement(
                errorPlace.hyperlinkText,
                containingElement,
                maxTokenCount,
                language,
                errorPlace.virtualFile
            ) ?: break

            containingElement = containingElement.parent
        }

        return result
    }

    private fun tryFitContainingElement(
        filename: String,
        currentContainingElement: PsiElement,
        maxTokenCount: Int,
        language: String,
        virtualFile: VirtualFile
    ): ErrorScope? {
        val lineNumberStart = ErrorPlacePsiUtils.getLineNumber(currentContainingElement, true)
        val lineNumberFinish = ErrorPlacePsiUtils.getLineNumber(currentContainingElement, false)

        val prefix = "filename: $filename\n line: $lineNumberStart\n\n"
        val candidate = """
            $prefix```$language
            ${currentContainingElement.text}
            ```
            """.trimIndent()

        return if (findTrimPositionForMaxTokens(candidate, maxTokenCount) >= candidate.length) {
            ErrorScope(
                lineNumberStart,
                lineNumberFinish,
                candidate,
                virtualFile
            )
        } else null
    }

    private fun tryFitAllFile(
        filename: String,
        programText: String,
        maxTokenCount: Int,
        language: String,
        virtualFile: VirtualFile
    ): ErrorScope? {
        val firstTry = "filename: $filename\n\n```$language\n$programText\n```\n"
        if (findTrimPositionForMaxTokens(firstTry, maxTokenCount) < firstTry.length) return null

        return ErrorScope(0, programText.lines().size - 1, firstTry, virtualFile)
    }

    private fun findTrimPositionForMaxTokens(text: String, maxTokenCount: Int): Int {
        var tokenSum = 0
        var trimPosition = 0
        for (line in text.lines()) {
            val tokenCountInLine = tokenizer.count(line) + 1
            if (tokenSum + tokenCountInLine > maxTokenCount) {
                break
            }

            trimPosition += line.length + 1
            tokenSum += tokenCountInLine
        }

        return trimPosition
    }

    private fun trimTextByTokenizer(text: String, maxTokenCount: Int): String {
        val offset = findTrimPositionForMaxTokens(text, maxTokenCount)
        return text.substring(0, min(text.length, offset))
    }

    companion object {
        fun buildDisplayPrompt(errorTextTrimmed: String, sourceCode: String, displayText: String): TextTemplatePrompt {
            val templateRender = TemplateRender(GENIUS_ERROR)
            templateRender.context = ErrorContext(errorTextTrimmed, sourceCode)
            val template = templateRender.getTemplate("fix-error.vm")
            val prompt = templateRender.renderTemplate(template)
            return TextTemplatePrompt(displayText, prompt)
        }
    }
}
