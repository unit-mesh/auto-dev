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
    override fun getPlaceholderText(node: ASTNode): String {
        val parent = node.treeParent ?: return node.text
        val prop = parent.findChildByType(DevInTypes.PROPERTY_VALUE) ?: return node.text

        return prop.text.split("/").last()
    }

    override fun buildFoldRegions(root: PsiElement, document: Document, quick: Boolean): Array<FoldingDescriptor> {
        val descriptors = ArrayList<FoldingDescriptor>()
        root.accept(object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                if (element.elementType == DevInTypes.AGENT_ID && element.text == "file" && isFileReference(element)) {
                    descriptors.add(FoldingDescriptor(element.node, element.textRange, null, emptySet(), true))
                }

                element.acceptChildren(this)
            }

            private fun isFileReference(element: PsiElement): Boolean {
                val text = element.text
                return text.split("/").isNotEmpty()
            }
        })

        return descriptors.toTypedArray()
    }
}
