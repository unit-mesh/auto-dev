package cc.unitmesh.devti.pair.arch

/**
 * Represents a node in a tree structure.
 *
 * @property name the name of the node
 * @property children the list of child nodes
 */
class TreeNode(val name: String, val children: MutableList<TreeNode> = mutableListOf()) {
    fun addChild(child: TreeNode) {
        children.add(child)
    }

    fun print(indent: String = "", isLast: Boolean = false): String {
        val sb = StringBuilder()
        sb.append(indent)
        sb.append(if (isLast) "└── " else "├── ")
        sb.append(name)
        sb.append("\n")

        val childIndent = "$indent${if (isLast) "    " else "│   "}"
        for ((index, child) in children.withIndex()) {
            val childIsLast = index == children.size - 1
            sb.append(child.print(childIndent, childIsLast))
        }

        return sb.toString()
    }
}
