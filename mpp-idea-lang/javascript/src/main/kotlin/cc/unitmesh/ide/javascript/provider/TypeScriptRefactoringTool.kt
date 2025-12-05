package cc.unitmesh.ide.javascript.provider

import cc.unitmesh.devti.provider.RefactorInstElement
import cc.unitmesh.devti.provider.RefactoringTool
import com.intellij.codeInsight.daemon.impl.quickfix.RenameElementFix
import com.intellij.lang.javascript.JavaScriptFileType
import com.intellij.lang.javascript.psi.JSFile
import com.intellij.lang.javascript.psi.JSFunction
import com.intellij.lang.javascript.psi.ecmal4.JSClass
import com.intellij.openapi.project.ProjectManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.util.PsiTreeUtil

class TypeScriptRefactoringTool : RefactoringTool {
    private val identifierPattern = Regex("^[a-zA-Z_][a-zA-Z0-9_]*$")

    val project = ProjectManager.getInstance().openProjects.firstOrNull()

    override fun lookupFile(path: String): PsiFile? {
        if (project == null) return null

        val searchScope = ProjectScope.getProjectScope(project)

        val jsFiles = FileTypeIndex.getFiles(JavaScriptFileType.INSTANCE, searchScope)
            .mapNotNull { PsiManager.getInstance(project).findFile(it) as? JSFile }

        val tsFiles = FileTypeIndex.getFiles(JavaScriptFileType.INSTANCE, searchScope)
            .mapNotNull { PsiManager.getInstance(project).findFile(it) as? JSFile }

        val files = jsFiles + tsFiles

        val sourceFile = files.firstOrNull {
            it.virtualFile.path == path
        } ?: return null

        return sourceFile
    }

    private fun findNamedElement(psiFile: PsiFile?, elementInfo: RefactorInstElement): PsiNamedElement? {
        return when (psiFile) {
            is JSFile -> findElementInJSFile(psiFile, elementInfo)
            else -> null
        }
    }

    private fun findElementInJSFile(jsFile: JSFile, elementInfo: RefactorInstElement): PsiNamedElement? {
        val classes = PsiTreeUtil.getChildrenOfTypeAsList(jsFile, JSClass::class.java)
        val functions = PsiTreeUtil.getChildrenOfTypeAsList(jsFile, JSFunction::class.java)

        return when {
            elementInfo.isClass -> findClassByName(classes, elementInfo.className)
            elementInfo.isMethod -> findMethodByName(classes, functions, elementInfo.methodName)
            else -> null
        }
    }

    private fun findClassByName(classes: List<JSClass>, className: String): PsiNamedElement? {
        return classes.firstOrNull { it.name == className }
    }

    private fun findMethodByName(
        classes: List<JSClass>,
        functions: List<JSFunction>,
        methodName: String
    ): PsiNamedElement? {
        return classes.firstNotNullOfOrNull {
            it.findFunctionByName(methodName)
        } ?: functions.firstOrNull { it.name == methodName }
    }

    override fun rename(sourceName: String, targetName: String, psiFile: PsiFile?): Boolean {
        if (project == null) return false
        // if targetElement is not a valid function name, return false
        if (!identifierPattern.matches(targetName)) {
            return false
        }

        val elementInfo = getElementInfo(sourceName, psiFile) ?: return false
        val element = findNamedElement(psiFile, elementInfo) ?: return false

        try {
            var target = targetName
            if (element is JSFile) {
                target += element.name.substringAfterLast(".")
            }

            RenameElementFix(element, target)
                .invoke(project, element.containingFile, element, element)

            performRefactoringRename(project, element, targetName)
        } catch (e: Exception) {
            return false
        }

        return false
    }

    private fun getElementInfo(input: String, psiFile: PsiFile?): RefactorInstElement? {
        if (!input.contains("#") && psiFile != null) {
            val jsFile = psiFile as? JSFile ?: return null
            // check input name is uppercase
            val isClass = input[0].isUpperCase()
            val isMethod = input[0].isLowerCase()

            return RefactorInstElement(isClass, isMethod, input, input, input, jsFile.name)
        }

        val isMethod = input.contains("#")
        val methodName = input.substringAfter("#")
        val canonicalName = input.substringBefore("#")
        val maybeClassName = canonicalName.substringAfterLast(".")
        // the clasName should be Uppercase or it will be the package
        var isClass = false
        var pkgName = canonicalName.substringBeforeLast(".")
        if (maybeClassName[0].isLowerCase()) {
            pkgName = "$pkgName.$maybeClassName"
        } else {
            isClass = true
        }

        return RefactorInstElement(isClass, isMethod, methodName, canonicalName, maybeClassName, pkgName)
    }

}
