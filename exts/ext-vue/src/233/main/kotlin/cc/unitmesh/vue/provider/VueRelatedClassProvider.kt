// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package cc.unitmesh.vue.provider

import cc.unitmesh.devti.provider.RelatedClassesProvider
import com.intellij.javascript.nodejs.PackageJsonData
import com.intellij.lang.ecmascript6.psi.*
import com.intellij.lang.ecmascript6.psi.impl.ES6ImportPsiUtil.ES6_IMPORT_DECLARATION
import com.intellij.lang.ecmascript6.resolve.ES6PsiUtil
import com.intellij.lang.ecmascript6.resolve.JSFileReferencesUtil
import com.intellij.lang.javascript.buildTools.npm.PackageJsonUtil
import com.intellij.lang.javascript.psi.ecma6.TypeScriptPropertySignature
import com.intellij.lang.javascript.psi.resolve.JSResolveUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.source.html.HtmlFileImpl
import com.intellij.psi.xml.XmlFile
import com.intellij.util.asSafely
import org.jetbrains.vuejs.index.findModule
import org.jetbrains.vuejs.index.findScriptTag
import java.util.*


/**
 * AutoDev Modular for Vue
 * based on [org.jetbrains.vuejs.web.scopes.VueCodeModelSymbolsScope]
 */
class VueRelatedClassProvider : RelatedClassesProvider {
    override fun lookup(element: PsiElement): List<PsiElement> {
        if (element !is XmlFile) return emptyList()

        return emptyList()
    }

    override fun lookup(psiFile: PsiFile): List<PsiElement> {
        if (psiFile !is XmlFile) return emptyList()

        val scriptTag = findScriptTag(psiFile, true) ?: findScriptTag(psiFile, false) ?: return emptyList()
        val localImports = sequenceOf(findModule(scriptTag, false), findModule(scriptTag, true))
            .flatMap {
                JSResolveUtil.getStubbedChildren(it, ES6_IMPORT_DECLARATION).asSequence()
            }
            .filterIsInstance<ES6ImportDeclaration>()
            .map {
                it.children.mapNotNull { source ->
                    when (source) {
                        is ES6ImportSpecifierAlias -> symbolLocationsFromSpecifier(source.findSpecifierElement() as? ES6ImportSpecifier)
                        is ES6ImportSpecifier -> symbolLocationsFromSpecifier(source)
                        is ES6ImportedBinding -> symbolLocationsForModule(source, source.declaration?.fromClause?.referenceText, "default")
                        is TypeScriptPropertySignature -> symbolLocationFromPropertySignature(source)?.let { listOf(it) }
                        is ES6ExportDefaultAssignment, is HtmlFileImpl -> source.containingFile.virtualFile?.url?.let {
                            listOf(WebTypesSymbolLocation(it, "default"))
                        }
                        else -> null
                    }

                }.flatten()
            }.flatten()
            .toList()

        return localImports.mapNotNull {
            it.virtualFile?.let { file ->
                if (!file.isValid || file.fileType.isBinary ) return@let null
                PsiManager.getInstance(psiFile.project).findFile(file)
            }
        }
    }

    private fun symbolLocationsFromSpecifier(specifier: ES6ImportSpecifier?): List<WebTypesSymbolLocation> {
        if (specifier?.specifierKind == ES6ImportExportSpecifier.ImportExportSpecifierKind.IMPORT) {
            val symbolName = if (specifier.isDefault) "default" else specifier.referenceName
            val moduleName = specifier.declaration?.fromClause?.referenceText
            return symbolLocationsForModule(specifier, moduleName, symbolName)
        }
        return emptyList()
    }

    private fun symbolLocationsForModule(context: PsiElement,
                                         moduleName: String?,
                                         symbolName: String?): List<WebTypesSymbolLocation> =
        if (symbolName != null && moduleName != null) {
            val result = mutableListOf<WebTypesSymbolLocation>()
            val unquotedModule = StringUtil.unquoteString(moduleName)
            if (!unquotedModule.startsWith(".")) {
                result.add(WebTypesSymbolLocation(unquotedModule.lowercase(Locale.US), symbolName))
            }

            if (unquotedModule.contains('/')) {
                val modules = JSFileReferencesUtil.resolveModuleReference(context, unquotedModule)
                modules.mapNotNullTo(result) {
                    val virtualFile = it.containingFile?.originalFile?.virtualFile
                    virtualFile?.path?.let { url ->
                        WebTypesSymbolLocation(url, symbolName, virtualFile)
                    }
                }
                // A workaround to avoid full resolution in case of components in subpackages
                if (symbolName == "default"
                    && !unquotedModule.startsWith(".")
                    && unquotedModule.count { it == '/' } == 1) {

                    modules.mapNotNullTo(result) {
                        ES6PsiUtil.findDefaultExport(it)
                            ?.asSafely<ES6ExportDefaultAssignment>()
                            ?.initializerReference
                            ?.let { symbolName ->
                                WebTypesSymbolLocation(unquotedModule.takeWhile { it != '/' }, symbolName)
                            }
                    }
                }
            }
            result
        }
        else emptyList()

    private fun symbolLocationFromPropertySignature(property: TypeScriptPropertySignature): WebTypesSymbolLocation? {
        if (!property.isValid) return null

        // TypeScript GlobalComponents definition
        val symbolName = property.memberName.takeIf { it.isNotEmpty() }
            ?: return null

        // Locate module
        val packageName =
            property.containingFile?.originalFile?.virtualFile?.let { PackageJsonUtil.findUpPackageJson(it) }
                ?.let { PackageJsonData.getOrCreate(it) }
                ?.name
                ?: return null

        return WebTypesSymbolLocation(packageName.lowercase(Locale.US), symbolName)
    }

    private data class WebTypesSymbolLocation(
        val moduleName: String,
        val symbolName: String,
        val virtualFile: VirtualFile? = null
    )
}
