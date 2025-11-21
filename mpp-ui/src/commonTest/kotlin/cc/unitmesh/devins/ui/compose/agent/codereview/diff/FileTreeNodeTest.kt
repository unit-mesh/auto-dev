package cc.unitmesh.devins.ui.compose.agent.codereview.diff

import cc.unitmesh.agent.diff.ChangeType
import cc.unitmesh.devins.ui.compose.agent.codereview.DiffFileInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FileTreeNodeTest {

    @Test
    fun `test single file in root`() {
        val files = listOf(
            DiffFileInfo(path = "README.md", changeType = ChangeType.EDIT)
        )

        val tree = buildFileTreeStructure(files)

        assertEquals(1, tree.size, "Should have 1 node in root")
        val node = tree[0]
        assertTrue(node is FileTreeNode.File, "Root level file should be File node, not Directory")
        assertEquals("README.md", node.name)
        assertEquals("README.md", node.path)
    }

    @Test
    fun `test multiple files in root`() {
        val files = listOf(
            DiffFileInfo(path = "README.md", changeType = ChangeType.EDIT),
            DiffFileInfo(path = "LICENSE", changeType = ChangeType.CREATE),
            DiffFileInfo(path = "build.gradle.kts", changeType = ChangeType.EDIT)
        )

        val tree = buildFileTreeStructure(files)

        assertEquals(3, tree.size, "Should have 3 nodes in root")
        tree.forEach { node ->
            assertTrue(node is FileTreeNode.File, "All root level nodes should be File nodes")
        }
    }

    @Test
    fun `test files in single directory`() {
        val files = listOf(
            DiffFileInfo(path = "src/Main.kt", changeType = ChangeType.EDIT, language = "Kotlin"),
            DiffFileInfo(path = "src/Utils.kt", changeType = ChangeType.CREATE, language = "Kotlin")
        )

        val tree = buildFileTreeStructure(files)

        assertEquals(1, tree.size, "Should have 1 directory in root")
        val srcDir = tree[0]
        assertTrue(srcDir is FileTreeNode.Directory, "src should be a Directory")
        assertEquals("src", srcDir.name)
        assertEquals("src", srcDir.path)
        assertEquals(2, srcDir.children.size, "src should have 2 children")

        val mainKt = srcDir.children.find { it.name == "Main.kt" }
        assertNotNull(mainKt, "Main.kt should exist")
        assertTrue(mainKt is FileTreeNode.File, "Main.kt should be a File")

        val utilsKt = srcDir.children.find { it.name == "Utils.kt" }
        assertNotNull(utilsKt, "Utils.kt should exist")
        assertTrue(utilsKt is FileTreeNode.File, "Utils.kt should be a File")
    }

    @Test
    fun `test nested directories`() {
        val files = listOf(
            DiffFileInfo(path = "src/main/kotlin/Main.kt", changeType = ChangeType.EDIT, language = "Kotlin"),
            DiffFileInfo(path = "src/main/kotlin/Utils.kt", changeType = ChangeType.CREATE, language = "Kotlin"),
            DiffFileInfo(path = "src/test/kotlin/MainTest.kt", changeType = ChangeType.CREATE, language = "Kotlin")
        )

        val tree = buildFileTreeStructure(files)

        assertEquals(1, tree.size, "Should have 1 directory in root")
        val srcDir = tree[0] as FileTreeNode.Directory
        assertEquals("src", srcDir.name)
        assertEquals(2, srcDir.children.size, "src should have 2 children (main and test)")

        val mainDir = srcDir.children.find { it.name == "main" } as? FileTreeNode.Directory
        assertNotNull(mainDir, "main directory should exist")
        assertEquals("src/main", mainDir.path)
        assertEquals(1, mainDir.children.size, "main should have 1 child (kotlin)")

        val mainKotlinDir = mainDir.children.find { it.name == "kotlin" } as? FileTreeNode.Directory
        assertNotNull(mainKotlinDir, "main/kotlin directory should exist")
        assertEquals(2, mainKotlinDir.children.size, "main/kotlin should have 2 files")
        assertTrue(mainKotlinDir.children.all { it is FileTreeNode.File }, "All children should be files")

        val testDir = srcDir.children.find { it.name == "test" } as? FileTreeNode.Directory
        assertNotNull(testDir, "test directory should exist")
        assertEquals("src/test", testDir.path)
    }

    @Test
    fun `test mixed root files and directories`() {
        val files = listOf(
            DiffFileInfo(path = "README.md", changeType = ChangeType.EDIT),
            DiffFileInfo(path = "src/Main.kt", changeType = ChangeType.EDIT, language = "Kotlin"),
            DiffFileInfo(path = "LICENSE", changeType = ChangeType.CREATE),
            DiffFileInfo(path = "src/Utils.kt", changeType = ChangeType.CREATE, language = "Kotlin")
        )

        val tree = buildFileTreeStructure(files)

        assertEquals(3, tree.size, "Should have 3 nodes in root (2 files + 1 directory)")

        val fileNodes = tree.filterIsInstance<FileTreeNode.File>()
        assertEquals(2, fileNodes.size, "Should have 2 files in root")
        assertTrue(fileNodes.any { it.name == "README.md" })
        assertTrue(fileNodes.any { it.name == "LICENSE" })

        val dirs = tree.filterIsInstance<FileTreeNode.Directory>()
        assertEquals(1, dirs.size, "Should have 1 directory in root")
        val srcDir = dirs[0]
        assertEquals("src", srcDir.name)
        assertEquals(2, srcDir.children.size, "src should have 2 files")
    }

    @Test
    fun `test complex directory structure`() {
        val files = listOf(
            DiffFileInfo(path = "mpp-ui/src/commonMain/kotlin/View.kt", changeType = ChangeType.EDIT, language = "Kotlin"),
            DiffFileInfo(path = "mpp-ui/src/jvmMain/kotlin/Platform.kt", changeType = ChangeType.EDIT, language = "Kotlin"),
            DiffFileInfo(path = "mpp-ui/build.gradle.kts", changeType = ChangeType.EDIT, language = "Kotlin"),
            DiffFileInfo(path = "README.md", changeType = ChangeType.EDIT)
        )

        val tree = buildFileTreeStructure(files)

        assertEquals(2, tree.size, "Should have 2 nodes in root")

        val readmeNode = tree.find { it.name == "README.md" }
        assertNotNull(readmeNode, "README.md should exist")
        assertTrue(readmeNode is FileTreeNode.File)

        val mppUiDir = tree.find { it.name == "mpp-ui" } as? FileTreeNode.Directory
        assertNotNull(mppUiDir, "mpp-ui directory should exist")
        assertEquals(2, mppUiDir.children.size, "mpp-ui should have 2 children (src + build.gradle.kts)")

        val buildFile = mppUiDir.children.find { it.name == "build.gradle.kts" }
        assertNotNull(buildFile, "build.gradle.kts should exist")
        assertTrue(buildFile is FileTreeNode.File)

        val srcDir = mppUiDir.children.find { it.name == "src" } as? FileTreeNode.Directory
        assertNotNull(srcDir, "src directory should exist")
    }

    @Test
    fun `test directory sorting - directories before files`() {
        val files = listOf(
            DiffFileInfo(path = "src/file.kt", changeType = ChangeType.EDIT, language = "Kotlin"),
            DiffFileInfo(path = "README.md", changeType = ChangeType.EDIT),
            DiffFileInfo(path = "docs/guide.md", changeType = ChangeType.CREATE)
        )

        val tree = buildFileTreeStructure(files)

        // Directories should come before files
        val sortedTree = tree.sortedWith(compareBy({ it !is FileTreeNode.Directory }, { it.name }))
        assertTrue(sortedTree[0] is FileTreeNode.Directory, "First should be a directory")
        assertTrue(sortedTree[1] is FileTreeNode.Directory, "Second should be a directory")
        assertTrue(sortedTree[2] is FileTreeNode.File, "Third should be a file")
    }

    @Test
    fun `test findDirectoryByPath`() {
        val root = mutableMapOf<String, FileTreeNode.Directory>()
        val srcDir = FileTreeNode.Directory("src", "src")
        val mainDir = FileTreeNode.Directory("main", "src/main")
        val kotlinDir = FileTreeNode.Directory("kotlin", "src/main/kotlin")

        root["src"] = srcDir
        srcDir.children.add(mainDir)
        mainDir.children.add(kotlinDir)

        // Test finding directory at different depths
        val foundSrc = findDirectoryByPath(root, "src")
        assertNotNull(foundSrc)
        assertEquals("src", foundSrc.name)

        val foundMain = findDirectoryByPath(root, "src/main")
        assertNotNull(foundMain)
        assertEquals("main", foundMain.name)

        val foundKotlin = findDirectoryByPath(root, "src/main/kotlin")
        assertNotNull(foundKotlin)
        assertEquals("kotlin", foundKotlin.name)

        // Test non-existent path
        val notFound = findDirectoryByPath(root, "src/main/java")
        assertEquals(null, notFound)
    }
}

