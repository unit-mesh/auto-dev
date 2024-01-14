package cc.unitmesh.idea.prompting

import cc.unitmesh.devti.context.model.DtClass
import cc.unitmesh.devti.provider.PromptStrategy
import cc.unitmesh.devti.prompting.CodePromptText
import cc.unitmesh.idea.fromJavaFile
import com.intellij.openapi.util.NlsSafe
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiJavaFile

class JvmPromptStrategy : PromptStrategy() {
    var tokenLength = tokenLength()

    override fun tokenLength(): Int {
        return 3072
    }

    override fun advice(prefixCode: String, suffixCode: String): CodePromptText {
        val tokenCount: Int = this.count(prefixCode)
        if (tokenCount < tokenLength) {
            return CodePromptText(prefixCode, suffixCode)
        }

        // remove all `import` syntax in java code, should contain with new line
        val importRegexWithNewLine = Regex("import .*\n")
        val prefixCodeWithoutImport = prefixCode.replace(importRegexWithNewLine, "")
        val tokenCountWithoutImport: Int = this.count(prefixCodeWithoutImport)

        if (tokenCountWithoutImport < tokenLength) {
            return CodePromptText(prefixCodeWithoutImport, suffixCode)
        }

        // keep only the service calling?
        return CodePromptText(prefixCodeWithoutImport, suffixCode)
    }

    override fun advice(psiFile: PsiElement, calleeName: String): CodePromptText {
        return when (psiFile) {
            is PsiJavaFile -> {
                adviceFile(psiFile, calleeName)
            }

            is PsiClass -> {
                adviceClass(psiFile, calleeName)
            }

            else -> {
                CodePromptText("", "")
            }
        }
    }

    private fun adviceFile(javaFile: PsiJavaFile, calleeName: String = ""): CodePromptText {
        val code = javaFile.text
        if (this.count(code) < tokenLength) {
            return CodePromptText(code, "")
        }

        // strategy 1: remove class code without the imports
        val javaCode = javaFile.classes[0]
        val countTokens = this.count(javaCode.text)
        if (countTokens < tokenLength) {
            return CodePromptText(javaCode.text, "")
        }

        return adviceClass(javaCode, calleeName)
    }

    fun adviceClass(javaCode: PsiClass, calleeName: String = ""): CodePromptText {
        val codeString = javaCode.text
        val textbase = advice(codeString, "")
        if (this.count(textbase.prefixCode) < tokenLength) {
            return CodePromptText(codeString, "")
        }

        // for Web controller, service, repository, etc.
        val fields = filterFieldsByName(javaCode, calleeName)
        if (fields.isEmpty()) {
            return CodePromptText(codeString, "")
        }

        val targetField = fields[0]
        val targetFieldRegex = Regex(".*\\s+$targetField\\..*")

        // strategy 2: if all method contains the field, we should return all method
        val methodCodes = getByMethods(javaCode, targetFieldRegex)
        if (this.count(methodCodes) < tokenLength) {
            return CodePromptText(methodCodes, "")
        }

        // strategy 3: if all line contains the field, we should return all line
        val lines = getByFields(codeString, targetFieldRegex)
        return CodePromptText(lines, "")
    }

    private fun filterFieldsByName(
        javaCode: PsiClass,
        calleeName: String
    ): List<@NlsSafe String> {
        val fields = javaCode.fields.filter {
            it.type.canonicalText == calleeName
        }.map {
            it.name
        }

        return fields
    }

    private fun getByMethods(javaCode: PsiClass, firstFieldRegex: Regex): String {
        val methods = javaCode.methods.filter {
            firstFieldRegex.containsMatchIn(it.text)
        }.map {
            it.text
        }

        val methodCodes = methods.joinToString("\n\n") {
            "    $it" // we need to add 4 spaces for each method
        }
        return methodCodes
    }

    /**
     * Retrieves all lines from the given code string that match the specified field regex.
     *
     * @param codeString The code string to search for field usage.
     * @param firstFieldRegex The regular expression pattern to match the first field usage in each line.
     * @return A string containing all the lines that match the field regex, with additional code added before each line.
     */
    private fun getByFields(
        codeString: @NlsSafe String,
        firstFieldRegex: Regex
    ): String {
        val lines = codeString.split("\n").filter {
            it.matches(firstFieldRegex)
        }

        return lines.joinToString("") { "        {some other code}\n$it\n" }
    }

    override fun advice(psiFile: PsiElement, usedMethod: List<String>, noExistMethods: List<String>): CodePromptText {
        return when (psiFile) {
            is PsiJavaFile -> {
                advice(psiFile, usedMethod, noExistMethods)
            }

            else -> {
                CodePromptText("", "")
            }
        }
    }

    /**
     * Generates a code prompt text for implementing missing methods in a given service file.
     *
     * @param serviceFile the PsiJavaFile representing the service file
     * @param usedMethod a list of used methods in the service file
     * @param noExistMethods a list of methods that do not exist in the service file
     * @return a CodePromptText object containing the generated code prompt text
     */
    fun advice(serviceFile: PsiJavaFile, usedMethod: List<String>, noExistMethods: List<String>): CodePromptText {
        val code = serviceFile.text
        val filterNeedImplementMethods = usedMethod.filter {
            noExistMethods.any { noExistMethod ->
                it.contains(noExistMethod)
            }
        }

        val suffixCode = filterNeedImplementMethods.joinToString(", ")

        val finalPrompt = code + """
            | // TODO: implement the method $suffixCode
        """.trimMargin()
        if (this.count(finalPrompt) < tokenLength) {
            return CodePromptText(code, suffixCode)
        }

        val javaCode = DtClass.fromJavaFile(serviceFile).format()

        return CodePromptText(javaCode, suffixCode)
    }
}