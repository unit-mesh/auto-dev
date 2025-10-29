package cc.unitmesh.devins.ui.swing

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.tree.*

class FileTreePanel(private val onFileSelected: (File) -> Unit) : JPanel(BorderLayout()) {
    
    private val tree: JTree
    private val treeModel: DefaultTreeModel
    private val rootNode: DefaultMutableTreeNode
    private var currentProjectRoot: File? = null
    
    init {
        // 创建根节点
        rootNode = DefaultMutableTreeNode("No Project")
        treeModel = DefaultTreeModel(rootNode)
        tree = JTree(treeModel)
        
        setupTree()
        setupUI()
    }
    
    private fun setupTree() {
        tree.apply {
            cellRenderer = FileTreeCellRenderer()
            isRootVisible = true
            showsRootHandles = true
            selectionModel.selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION
            
            // 双击打开文件
            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    if (e.clickCount == 2) {
                        val path = selectionPath
                        if (path != null) {
                            val node = path.lastPathComponent as DefaultMutableTreeNode
                            val file = node.userObject as? File
                            if (file != null && file.isFile && isDevInsFile(file)) {
                                onFileSelected(file)
                            }
                        }
                    }
                }
            })
        }
    }
    
    private fun setupUI() {
        border = EmptyBorder(0, 0, 0, 0)
        
        // 标题栏
        val headerPanel = JPanel(BorderLayout()).apply {
            background = UIManager.getColor("Panel.background")
            border = EmptyBorder(8, 12, 8, 12)
            preferredSize = Dimension(0, 40)
        }
        
        val titleLabel = JLabel("📁 Project Explorer").apply {
            font = font.deriveFont(Font.BOLD, 13f)
        }
        
        val openButton = JButton("Open").apply {
            preferredSize = Dimension(60, 24)
            font = font.deriveFont(11f)
            addActionListener { openProject() }
        }
        
        headerPanel.add(titleLabel, BorderLayout.WEST)
        headerPanel.add(openButton, BorderLayout.EAST)
        
        // 树形视图
        val scrollPane = JScrollPane(tree).apply {
            border = null
            verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
            horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        }
        
        add(headerPanel, BorderLayout.NORTH)
        add(scrollPane, BorderLayout.CENTER)
    }
    
    private fun openProject() {
        val fileChooser = JFileChooser().apply {
            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
            dialogTitle = "Select Project Directory"
            currentDirectory = currentProjectRoot ?: File(System.getProperty("user.home"))
        }
        
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            val selectedDir = fileChooser.selectedFile
            loadProject(selectedDir)
        }
    }
    
    fun loadProject(projectDir: File) {
        if (!projectDir.exists() || !projectDir.isDirectory) {
            JOptionPane.showMessageDialog(
                this,
                "Selected path is not a valid directory",
                "Error",
                JOptionPane.ERROR_MESSAGE
            )
            return
        }
        
        currentProjectRoot = projectDir
        
        SwingUtilities.invokeLater {
            // 清空现有节点
            rootNode.removeAllChildren()
            rootNode.userObject = projectDir.name
            
            // 加载目录结构
            loadDirectoryStructure(projectDir, rootNode)
            
            // 刷新树
            treeModel.reload()
            
            // 展开根节点
            tree.expandPath(TreePath(rootNode.path))
        }
    }
    
    private fun loadDirectoryStructure(dir: File, parentNode: DefaultMutableTreeNode) {
        try {
            val files = dir.listFiles() ?: return
            
            // 先添加目录，再添加文件
            val directories = files.filter { it.isDirectory && !it.isHidden }.sortedBy { it.name.lowercase() }
            val regularFiles = files.filter { it.isFile && !it.isHidden }.sortedBy { it.name.lowercase() }
            
            // 添加目录
            directories.forEach { subDir ->
                if (shouldIncludeDirectory(subDir)) {
                    val dirNode = DefaultMutableTreeNode(subDir)
                    parentNode.add(dirNode)
                    loadDirectoryStructure(subDir, dirNode)
                }
            }
            
            // 添加文件
            regularFiles.forEach { file ->
                if (shouldIncludeFile(file)) {
                    val fileNode = DefaultMutableTreeNode(file)
                    parentNode.add(fileNode)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun shouldIncludeDirectory(dir: File): Boolean {
        val name = dir.name
        return !name.startsWith(".") && 
               name != "node_modules" && 
               name != "build" && 
               name != "target" && 
               name != ".gradle" &&
               name != ".idea"
    }
    
    private fun shouldIncludeFile(file: File): Boolean {
        return isDevInsFile(file) || isCommonTextFile(file)
    }
    
    private fun isDevInsFile(file: File): Boolean {
        val extension = file.extension.lowercase()
        return extension in setOf("devin", "devins", "md", "txt")
    }
    
    private fun isCommonTextFile(file: File): Boolean {
        val extension = file.extension.lowercase()
        return extension in setOf(
            "kt", "java", "js", "ts", "py", "go", "rs", "cpp", "c", "h",
            "json", "yaml", "yml", "xml", "html", "css", "scss", "less",
            "gradle", "properties", "conf", "config", "toml"
        )
    }
}

/**
 * 自定义文件树渲染器
 */
class FileTreeCellRenderer : DefaultTreeCellRenderer() {
    
    override fun getTreeCellRendererComponent(
        tree: JTree,
        value: Any,
        sel: Boolean,
        expanded: Boolean,
        leaf: Boolean,
        row: Int,
        hasFocus: Boolean
    ): Component {
        
        super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus)
        
        val node = value as DefaultMutableTreeNode
        val file = node.userObject as? File
        
        if (file != null) {
            text = file.name
            icon = getFileIcon(file, expanded)
        } else {
            text = node.userObject.toString()
            icon = if (expanded) openIcon else closedIcon
        }
        
        return this
    }
    
    private fun getFileIcon(file: File, expanded: Boolean): Icon? {
        return when {
            file.isDirectory -> if (expanded) openIcon else closedIcon
            isDevInsFile(file) -> createColoredIcon("📄", Color(100, 150, 255))
            file.extension.lowercase() in setOf("kt", "java") -> createColoredIcon("☕", Color(255, 165, 0))
            file.extension.lowercase() in setOf("js", "ts") -> createColoredIcon("🟨", Color(255, 215, 0))
            file.extension.lowercase() in setOf("py") -> createColoredIcon("🐍", Color(76, 175, 80))
            file.extension.lowercase() in setOf("json", "yaml", "yml") -> createColoredIcon("⚙️", Color(158, 158, 158))
            else -> leafIcon
        }
    }
    
    private fun isDevInsFile(file: File): Boolean {
        val extension = file.extension.lowercase()
        return extension in setOf("devin", "devins")
    }
    
    private fun createColoredIcon(emoji: String, color: Color): Icon {
        return object : Icon {
            override fun paintIcon(c: Component?, g: Graphics?, x: Int, y: Int) {
                g?.let {
                    val g2 = it as Graphics2D
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                    g2.font = Font("Segoe UI Emoji", Font.PLAIN, 12)
                    g2.color = color
                    g2.drawString(emoji, x, y + 12)
                }
            }
            
            override fun getIconWidth() = 16
            override fun getIconHeight() = 16
        }
    }
}
