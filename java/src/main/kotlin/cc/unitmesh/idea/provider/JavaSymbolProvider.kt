package cc.unitmesh.idea.provider

import cc.unitmesh.devti.devins.provider.ShireSymbolProvider
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionUtil
import com.intellij.codeInsight.completion.CompletionUtilCore
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.impl.file.impl.JavaFileManagerImpl
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.search.PsiShortNamesCache
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.SmartList

class JavaSymbolProvider : ShireSymbolProvider {
    override val language: String = JavaLanguage.INSTANCE.displayName

    override fun lookupSymbol(
        project: Project,
        parameters: CompletionParameters,
        result: CompletionResultSet,
    ): List<LookupElement> {
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
            if (it.packageName.startsWith(text)) {
                val element = LookupElementBuilder.create(it.packageName).withIcon(JavaFileType.INSTANCE.icon)
                lookupElements.add(element)
            }
        }

        return lookupElements
    }

    override fun lookupElementByName(project: Project, name: String): List<PsiElement>? {
        val searchScope = ProjectScope.getProjectScope(project)
        val virtualFiles = FileTypeIndex.getFiles(JavaFileType.INSTANCE, searchScope)
        val psiManager = PsiManager.getInstance(project)

        return when (name) {
            "PsiFile" -> virtualFiles.mapNotNull(psiManager::findFile).toList()
            "PsiPackage" -> virtualFiles.mapNotNull { psiManager.findFile(it) }
                .flatMap { PsiTreeUtil.getChildrenOfTypeAsList(it, PsiPackageStatement::class.java) }
                .toList()

            "PsiClass" -> virtualFiles.mapNotNull { psiManager.findFile(it) as PsiJavaFile }
                .flatMap { it.classes.toList() }
                .toList()

            "PsiMethod" -> virtualFiles.mapNotNull { psiManager.findFile(it) as PsiJavaFile }
                .flatMap { it.classes.toList() }
                .flatMap { it.methods.toList() }

            "PsiField" -> virtualFiles.mapNotNull { psiManager.findFile(it) as PsiJavaFile }
                .flatMap { it.classes.toList() }
                .flatMap { it.fields.toList() }

            else -> {
                null
            }
        }
    }

    override fun resolveSymbol(project: Project, symbol: String): List<PsiNamedElement> {
        val scope = GlobalSearchScope.allScope(project)

        if (symbol.isEmpty()) return emptyList()

        // className only, like `String` not Dot
        if (symbol.contains(".").not()) {
            val psiClasses = PsiShortNamesCache.getInstance(project).getClassesByName(symbol, scope)
            if (psiClasses.isNotEmpty()) {
                return psiClasses.toList()
            }
        }

        // for package name only, like `cc.unitmesh`
        JavaFileManagerImpl(project).findPackage(symbol)?.let { pkg ->
            return pkg.classes.toList()
        }

        // for single class, with function name, like `cc.unitmesh.idea.provider.JavaCustomShireSymbolProvider`
        val clazz = JavaFileManagerImpl(project).findClass(symbol, scope)
        if (clazz != null) {
            return clazz.methods.toList()
        }

        // for lookup for method
        val method = symbol.split("#")
        if (method.size == 2) {
            val clazzName = method[0]
            val methodName = method[1]
            return lookupWithMethodName(project, clazzName, scope, methodName)
        }

        // may by not our format, like <package>.<class>.<method> split last
        val lastDotIndex = symbol.lastIndexOf(".")
        if (lastDotIndex != -1) {
            val clazzName = symbol.substring(0, lastDotIndex)
            val methodName = symbol.substring(lastDotIndex + 1)
            return lookupWithMethodName(project, clazzName, scope, methodName)
        }

        return emptyList()
    }

    private fun lookupWithMethodName(
        project: Project,
        clazzName: String,
        scope: GlobalSearchScope,
        methodName: String,
    ): List<PsiMethod> {
        val psiClass = JavaFileManagerImpl(project).findClass(clazzName, scope) ?: return emptyList()
        val psiMethod = ApplicationManager.getApplication().executeOnPooledThread<PsiMethod?> {
            runReadAction { psiClass.findMethodsByName(methodName, true).firstOrNull() }
        }.get() ?: return emptyList()

        return listOf(psiMethod)
    }
}

