// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.temporary.error

import com.intellij.temporary.AutoPsiUtils
import cc.unitmesh.devti.llms.tokenizer.Tokenizer
import cc.unitmesh.devti.prompting.BasePromptText
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import java.lang.String.format
import kotlin.math.min

class ErrorPromptBuilder(private val maxLength: Int, private val tokenizer: Tokenizer) {
    private val promptTemplate =
        "As a helpful assistant with expertise in code debugging, your objective is to identify the roots of runtime problems by analyzing console logs and providing general solutions to fix the issues. When assisting users, follow these rules:\n\n1. Always be helpful and professional.\n2. Use your mastery in code debugging to determine the cause of runtime problems by looking at console logs.\n3. Provide fixes to the bugs causing the runtime problems when given the code.\n4. Ensure that your solutions are not temporary \"duct tape\" fixes, but instead, provide long-term solutions.\n5. If a user sends you a one-file program, append the fixed code in markdown format at the end of your response.\nThis code will be extracted using re.findall(r\"`{{3}}(\\w*)\\n([\\S\\s]+?)\\n`{{3}}\", model_response)\nso adhere to this formatting strictly.\n6. If you can fix the problem strictly by modifying the code, do so. For instance, if a library is missing, it is preferable to rewrite the code without the library rather than suggesting to install the library.\n7. Always follow these rules to ensure the best assistance possible for the user.\n\nNow, consider this user request:\n\n\"Please help me understand what the problem is and try to fix the code. Here's the console output and the program text:\n\nConsole output:\n%s\nTexts of programs:\n%s\nProvide a helpful response that addresses the user's concerns, adheres to the rules, and offers a solution for the runtime problem."
    private val displayText =
        "Please help me understand what the problem is and try to fix the code. Here's the console output and the program text:\nConsole output:\n%s\nTexts of programs:\n%s"

    @JvmSynthetic
    fun buildPrompt(errorText: String, list: List<ErrorPlace>): BasePromptText {
        var sourceCode = ""
        val maxLengthForPiece = (maxLength - (promptTemplate.length - 10)) / 2
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

        val formattedPrompt = format(promptTemplate, "```\n$errorTextTrimmed\n```\n", sourceCode)
        val formattedDisplayText = format(displayText, "```\n$errorTextTrimmed\n```\n", sourceCode)

        return BasePromptText(formattedDisplayText, formattedPrompt)
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
        val lineNumberStart = AutoPsiUtils.getLineNumber(currentContainingElement, true)
        val lineNumberFinish = AutoPsiUtils.getLineNumber(currentContainingElement, false)

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
}
