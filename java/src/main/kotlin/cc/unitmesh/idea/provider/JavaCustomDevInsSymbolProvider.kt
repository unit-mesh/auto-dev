package cc.unitmesh.idea.provider

import cc.unitmesh.devti.provider.devins.DevInsSymbolProvider
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.lang.java.JavaLanguage
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


class JavaCustomDevInsSymbolProvider : DevInsSymbolProvider {
    override val language: String = JavaLanguage.INSTANCE.displayName

    /**
     * Spike use `PackageIndex` to get all package name, maybe fast?
     * 
     * ```kotlin
     * PackageIndex.getInstance(project).getDirectoriesByPackageName(text, true).forEach {
     *     val element = LookupElementBuilder.create(it.name).withIcon(JavaFileType.INSTANCE.icon)
     *     lookupElements.add(element)
     * }
     * ```
     */
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
                val element = LookupElementBuilder.create(it.packageName)
                    .withIcon(JavaFileType.INSTANCE.icon)
                    .withTypeText("package")
                lookupElements.add(element)
            }
        }

        val psiShortNamesCache = PsiShortNamesCache.getInstance(project)
        val classNames = psiShortNamesCache.allClassNames

        classNames.forEach { className ->
            if (className.startsWith(text) || text.isEmpty()) {
                val psiClasses = psiShortNamesCache.getClassesByName(className, searchScope)
                psiClasses.forEach { psiClass ->
                    val qualifiedName = psiClass.qualifiedName ?: return@forEach
                    val element = LookupElementBuilder.create(qualifiedName)
                        .withIcon(JavaFileType.INSTANCE.icon)
                        .withTypeText("class")
                    lookupElements.add(element)
                }
            }
        }

        return lookupElements
    }

    override fun resolveSymbol(project: Project, symbol: String): List<String> {
        val scope = ProjectScope.getProjectScope(project)

        if (symbol.isEmpty()) return emptyList()

        if (symbol.contains(".").not()) {
            val psiClasses = PsiShortNamesCache.getInstance(project).getClassesByName(symbol, scope)
            if (psiClasses.isNotEmpty()) {
                return psiClasses.mapNotNull { it.qualifiedName }
            }
        }

        JavaFileManagerImpl(project).findPackage(symbol)?.let { pkg ->
            return pkg.classes.mapNotNull { it.qualifiedName }
        }

        val clazz = JavaFileManagerImpl(project).findClass(symbol, scope)
        if (clazz != null) {
            val classInfo = mutableListOf(clazz.qualifiedName ?: "")
            classInfo.addAll(clazz.methods.map { "${clazz.qualifiedName ?: ""}#${it.name}" })
            classInfo.addAll(clazz.fields.map { "${clazz.qualifiedName ?: ""}.${it.name}" })
            return classInfo
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


    override fun resolveElement(project: Project, symbol: String): List<PsiElement> {
        val scope = ProjectScope.getProjectScope(project)

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
            return runReadAction { pkg.classes.toList() }
        }

        // for single class, with function name, like `cc.unitmesh.idea.provider.JavaCustomDevInsSymbolProvider`
        val clazz = JavaFileManagerImpl(project).findClass(symbol, scope)
        if (clazz != null) {
            return listOf(clazz)
        }

        // for lookup for method
        val split = symbol.split("#")
        if (split.size == 2) {
            val clazzName = split[0]
            val methodName = split[1]
            return lookupElementWithMethodName(project, clazzName, scope, methodName)
        }

        // for lookup class field
        val fieldSplit = symbol.split(".")
        if (fieldSplit.size >= 2) {
            val className = fieldSplit.dropLast(1).joinToString(".")
            val fieldName = fieldSplit.last()
            
            val psiClass = JavaFileManagerImpl(project).findClass(className, scope)
            if (psiClass != null) {
                val field = psiClass.findFieldByName(fieldName, true)
                if (field != null) {
                    return listOf(field)
                }
            }
        }

        // may by not our format, like <package>.<class>.<method> split last
        val lastDotIndex = symbol.lastIndexOf(".")
        if (lastDotIndex != -1) {
            val clazzName = symbol.substring(0, lastDotIndex)
            val methodName = symbol.substring(lastDotIndex + 1)
            return lookupElementWithMethodName(project, clazzName, scope, methodName)
        }

        return emptyList()
    }


    private fun lookupWithMethodName(
        project: Project,
        clazzName: String,
        scope: GlobalSearchScope,
        methodName: String
    ): List<String> {
        val psiClass = JavaFileManagerImpl(project).findClass(clazzName, scope)
        if (psiClass != null) {
            val psiMethod = psiClass.findMethodsByName(methodName, true).firstOrNull()
            if (psiMethod != null) {
                return listOf(psiMethod.text)
            }
        }

        return emptyList()
    }

    private fun lookupElementWithMethodName(
        project: Project,
        clazzName: String,
        scope: GlobalSearchScope,
        methodName: String
    ): List<PsiElement> {
        val psiClass = JavaFileManagerImpl(project).findClass(clazzName, scope)
        if (psiClass != null) {
            val psiMethod = psiClass.findMethodsByName(methodName, true).firstOrNull()
            if (psiMethod != null) {
                return listOf(psiMethod)
            }
        }

        return emptyList()
    }
}
