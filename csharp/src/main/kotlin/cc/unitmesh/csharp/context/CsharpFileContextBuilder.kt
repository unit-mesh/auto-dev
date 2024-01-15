package cc.unitmesh.csharp.context

import cc.unitmesh.devti.context.FileContext
import cc.unitmesh.devti.context.builder.FileContextBuilder
import com.intellij.psi.PsiFile
import com.jetbrains.rider.ideaInterop.fileTypes.csharp.psi.CSharpFile

class CsharpFileContextBuilder: FileContextBuilder {
    override fun getFileContext(psiFile: PsiFile): FileContext? {
        if(psiFile !is CSharpFile) return null


        return null
    }
}
