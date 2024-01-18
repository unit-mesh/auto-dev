package cc.unitmesh.python.context

import cc.unitmesh.devti.context.FileContext
import cc.unitmesh.devti.context.builder.FileContextBuilder
import com.intellij.psi.PsiFile
import com.intellij.psi.util.childrenOfType
import com.jetbrains.python.psi.PyClass
import com.jetbrains.python.psi.PyFunction
import com.jetbrains.python.psi.PyImportStatementBase

class PythonFileContextBuilder : FileContextBuilder {
    override fun getFileContext(psiFile: PsiFile): FileContext {
        val importStatements = psiFile.childrenOfType<PyImportStatementBase>()
            .flatMap { it.importElements.toList() }

        val classNames = psiFile.childrenOfType<PyClass>()
        val functionNames = psiFile.childrenOfType<PyFunction>()

        return FileContext(
            root = psiFile,
            name = psiFile.name,
            path = psiFile.virtualFile.path,
            imports = importStatements,
            classes = classNames,
            methods = functionNames,
            packageString = null
        )
    }
}
