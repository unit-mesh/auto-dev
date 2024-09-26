package cc.unitmesh.devti.intentions.action.test

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

class TestCodeGenRequest(
    val file: PsiFile,
    val element: PsiElement,
    val project: Project,
    val editor: Editor?
)