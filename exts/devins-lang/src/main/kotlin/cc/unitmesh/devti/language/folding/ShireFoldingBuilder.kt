package cc.unitmesh.devti.language.folding

import cc.unitmesh.devti.command.dataprovider.BuiltinCommand
import cc.unitmesh.devti.language.psi.DevInCaseBody
import cc.unitmesh.devti.language.psi.DevInQueryStatement
import cc.unitmesh.devti.language.psi.DevInTypes
import cc.unitmesh.devti.language.psi.DevInUsed
import cc.unitmesh.devti.language.psi.DevInVisitor
import cc.unitmesh.devti.language.utils.lookupFile
import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiUtilCore
import com.intellij.psi.util.elementType

class ShireFoldingBuilder : FoldingBuilderEx() {
    override fun isCollapsedByDefault(node: ASTNode): Boolean = true
    override fun getPlaceholderText(node: ASTNode): String = node.text

    override fun buildFoldRegions(root: PsiElement, document: Document, quick: Boolean): Array<FoldingDescriptor> {
        val descriptors = mutableListOf<FoldingDescriptor>()
        root.accept(DevInFoldingVisitor(descriptors))
        return descriptors.toTypedArray()
    }

    override fun getPlaceholderText(node: ASTNode, range: TextRange): String {
        val elementType = PsiUtilCore.getElementType(node)
        when (elementType) {
            DevInTypes.USED -> {
                val commandId = (node.psi as DevInUsed).commandId
                when (commandId?.text) {
                    BuiltinCommand.FILE.commandName -> {
                        val prop = (node.psi as DevInUsed).commandProp?.text ?: return ""
                        val virtualFile = file((node.psi as DevInUsed).project, prop)
                        return "/${BuiltinCommand.FILE.commandName}:${virtualFile?.name}"
                    }
                    BuiltinCommand.STRUCTURE.commandName -> {
                        val prop = (node.psi as DevInUsed).commandProp?.text ?: return ""
                        val virtualFile = file((node.psi as DevInUsed).project, prop)
                        return "/${BuiltinCommand.STRUCTURE.commandName}:${virtualFile?.name}"
                    }
                }
            }
        }

        val explicitName = foldedElementsPresentations[elementType]
        val elementText = StringUtil.shortenTextWithEllipsis(node.text, 30, 5)
        return explicitName?.let { "$it: $elementText" } ?: elementText
    }

    private val foldedElementsPresentations = hashMapOf(
        DevInTypes.FRONT_MATTER_HEADER to "Hobbit Hole",
        DevInTypes.CODE to "Code Block",
        DevInTypes.QUERY_STATEMENT to "DevIn AstQL",
        DevInTypes.BLOCK_COMMENT to "/* ... */",
    )

    override fun isCollapsedByDefault(foldingDescriptor: FoldingDescriptor): Boolean {
        return when (foldingDescriptor.element.elementType) {
            DevInTypes.FRONT_MATTER_HEADER -> true
            DevInTypes.CODE -> false
            DevInTypes.USED -> true
            else -> false
        }
    }
}

fun file(project: Project, path: String): VirtualFile? {
    val filename = path.split("#")[0]
    val virtualFile = project.lookupFile(filename)
    return virtualFile
}


class DevInFoldingVisitor(private val descriptors: MutableList<FoldingDescriptor>) : DevInVisitor() {
    override fun visitElement(element: PsiElement) {
        when (element.elementType) {
            DevInTypes.FRONT_MATTER_HEADER -> {
                descriptors.add(FoldingDescriptor(element.node, element.textRange))
            }

            DevInTypes.CODE -> {
                descriptors.add(FoldingDescriptor(element.node, element.textRange))
            }

            DevInTypes.USED -> {
                val commandId = (element as? DevInUsed)?.commandId
                if (commandId?.text == BuiltinCommand.FILE.commandName) {
                    descriptors.add(FoldingDescriptor(element.node, element.textRange))
                }
            }
        }

        element.acceptChildren(this)
    }

    override fun visitQueryStatement(o: DevInQueryStatement) {
        descriptors.add(FoldingDescriptor(o.node, o.textRange))
    }

    override fun visitCaseBody(o: DevInCaseBody) {
        descriptors.add(FoldingDescriptor(o.node, o.textRange))
    }
}
