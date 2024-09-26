package cc.unitmesh.devti.pair.arch

class ProjectPackageTree {
    private var tree: TreeNode? = null

    private fun getTree(): TreeNode {
        if (tree == null) {
            tree = TreeNode("root")
        }
        return tree!!
    }

    fun setTree(tree: TreeNode) {
        this.tree = tree
    }

    fun addPackage(packageName: String) {
        val tree = getTree()
        val packages = packageName.split(".")
        var node = tree
        for (pkg in packages) {
            val child = node.children.find { it.name == pkg }
            if (child != null) {
                node = child
            } else {
                val newNode = TreeNode(pkg)
                node.addChild(newNode)
                node = newNode
            }
        }
    }

    fun print(): String {
        return getTree().print()
    }
}

