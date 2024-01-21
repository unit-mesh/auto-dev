package cc.unitmesh.devti.pair.arch

class TreeNode(val name: String, val children: MutableList<TreeNode> = mutableListOf()) {
    fun addChild(child: TreeNode) {
        children.add(child)
    }

    fun print(): String {
        val sb = StringBuilder()
        sb.append(name)
        sb.append("\n")
        for (child in children) {
            sb.append(child.print())
        }
        return sb.toString()
    }
}