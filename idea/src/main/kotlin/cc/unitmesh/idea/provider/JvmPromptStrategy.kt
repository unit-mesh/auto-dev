package cc.unitmesh.idea.provider

import cc.unitmesh.devti.context.model.DtClass
import cc.unitmesh.devti.provider.PromptStrategy
import cc.unitmesh.devti.prompting.model.FinalCodePrompt
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

    override fun advice(prefixCode: String, suffixCode: String): FinalCodePrompt {
        val tokenCount: Int = this.countTokens(prefixCode)
        if (tokenCount < tokenLength) {
            return FinalCodePrompt(prefixCode, suffixCode)
        }

        // remove all `import` syntax in java code, should contain with new line
        val importRegexWithNewLine = Regex("import .*\n")
        val prefixCodeWithoutImport = prefixCode.replace(importRegexWithNewLine, "")
        val tokenCountWithoutImport: Int = this.countTokens(prefixCodeWithoutImport)

        if (tokenCountWithoutImport < tokenLength) {
            return FinalCodePrompt(prefixCodeWithoutImport, suffixCode)
        }

        // keep only the service calling?
        return FinalCodePrompt(prefixCodeWithoutImport, suffixCode)
    }

    override fun advice(psiFile: PsiElement, calleeName: String): FinalCodePrompt {
        return when (psiFile) {
            is PsiJavaFile -> {
                adviceFile(psiFile, calleeName)
            }

            is PsiClass -> {
                adviceClass(psiFile, calleeName)
            }

            else -> {
                FinalCodePrompt("", "")
            }
        }
    }

    private fun adviceFile(javaFile: PsiJavaFile, calleeName: String = ""): FinalCodePrompt {
        val code = javaFile.text
        if (this.countTokens(code) < tokenLength) {
            return FinalCodePrompt(code, "")
        }

        // strategy 1: remove class code without the imports
        val javaCode = javaFile.classes[0]
        val countTokens = this.countTokens(javaCode.text)
        if (countTokens < tokenLength) {
            return FinalCodePrompt(javaCode.text, "")
        }

        return adviceClass(javaCode, calleeName)
    }

    fun adviceClass(javaCode: PsiClass, calleeName: String = ""): FinalCodePrompt {
        val codeString = javaCode.text
        val textbase = advice(codeString, "")
        if (this.countTokens(textbase.prefixCode) < tokenLength) {
            return FinalCodePrompt(codeString, "")
        }

        // for Web controller, service, repository, etc.
        val fields = filterFieldsByName(javaCode, calleeName)
        if (fields.isEmpty()) {
            return FinalCodePrompt(codeString, "")
        }

        val targetField = fields[0]
        val targetFieldRegex = Regex(".*\\s+$targetField\\..*")

        // strategy 2: if all method contains the field, we should return all method
        val methodCodes = getByMethods(javaCode, targetFieldRegex)
        if (this.countTokens(methodCodes) < tokenLength) {
            return FinalCodePrompt(methodCodes, "")
        }

        // strategy 3: if all line contains the field, we should return all line
        val lines = getByFields(codeString, targetFieldRegex)
        return FinalCodePrompt(lines, "")
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

    // get all line when match by field usage (by Regex)
    private fun getByFields(
        codeString: @NlsSafe String,
        firstFieldRegex: Regex
    ): String {
        val lines = codeString.split("\n").filter {
            it.matches(firstFieldRegex)
        }

        return lines.joinToString("") { "        {some other code}\n$it\n" }
    }

    override fun advice(psiFile: PsiElement, usedMethod: List<String>, noExistMethods: List<String>): FinalCodePrompt {
        return when (psiFile) {
            is PsiJavaFile -> {
                advice(psiFile, usedMethod, noExistMethods)
            }

            else -> {
                FinalCodePrompt("", "")
            }
        }
    }

    fun advice(serviceFile: PsiJavaFile, usedMethod: List<String>, noExistMethods: List<String>): FinalCodePrompt {
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
        if (this.countTokens(finalPrompt) < tokenLength) {
            return FinalCodePrompt(code, suffixCode)
        }

        val javaCode = DtClass.fromJavaFile(serviceFile).format()

        return FinalCodePrompt(javaCode, suffixCode)
    }
}