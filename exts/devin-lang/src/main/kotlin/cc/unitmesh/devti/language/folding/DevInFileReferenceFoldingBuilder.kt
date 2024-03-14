package cc.unitmesh.devti.language.folding

import cc.unitmesh.devti.language.psi.DevInTypes
import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.util.elementType

class DevInFileReferenceFoldingBuilder : FoldingBuilderEx() {
    override fun isCollapsedByDefault(node: ASTNode): Boolean = true
    override fun getPlaceholderText(node: ASTNode): String =
        if (node.elementType == DevInTypes.PROPERTY_VALUE) {
            node.text.split("/").last()
        } else {
            node.text
        }

    override fun buildFoldRegions(root: PsiElement, document: Document, quick: Boolean): Array<FoldingDescriptor> {
        val descriptors = arrayListOf<FoldingDescriptor>()
        root.accept(object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                if (element.elementType == DevInTypes.PROPERTY_VALUE) {
                    val agentId = element.parent?.findElementAt(1)?.text
                    if (agentId == "file" && element.text.contains("/")) {
                        descriptors.add(
                            FoldingDescriptor(element.node, element.textRange, null, emptySet(), true)
                        )
                    }
                }

                element.acceptChildren(this)
            }
        })

        return descriptors.toTypedArray()
    }
}
