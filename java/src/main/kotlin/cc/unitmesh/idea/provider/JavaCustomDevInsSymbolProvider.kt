package cc.unitmesh.idea.provider

import cc.unitmesh.devti.provider.devins.DevInsSymbolProvider
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiPackageStatement
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.SmartList

class JavaCustomDevInsSymbolProvider : DevInsSymbolProvider {
    override fun lookupSymbol(
        project: Project,
        parameters: CompletionParameters,
        result: CompletionResultSet,
    ): Iterable<LookupElement> {
        val lookupElements: MutableList<LookupElement> = SmartList()
        val searchScope = ProjectScope.getProjectScope(project)
        val javaFiles = FileTypeIndex.getFiles(JavaFileType.INSTANCE, searchScope)
        if (javaFiles.isEmpty()) return lookupElements

        val prefixMatcher = CompletionUtil.findReferenceOrAlphanumericPrefix(parameters)
        result.withPrefixMatcher(prefixMatcher)

        val text = parameters.position.text.removePrefix(CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED)

        val packageStatements = javaFiles.mapNotNull {
            val psi = PsiManager.getInstance(project).findFile(it) ?: return@mapNotNull null
            PsiTreeUtil.getChildrenOfTypeAsList(psi, PsiPackageStatement::class.java).firstOrNull()
        }

        packageStatements.forEach {
            val packageName = it.packageName
            if (packageName != null && packageName.startsWith(text)) {
                lookupElements.add(LookupElementBuilder.create(packageName))
            }
        }

        return lookupElements
    }
}
