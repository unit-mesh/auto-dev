package cc.unitmesh.devins.ui.compose.document

import cc.unitmesh.devins.document.DocumentFile
import cc.unitmesh.devins.document.DocumentFolder
import cc.unitmesh.devins.document.DocumentTreeNode
import cc.unitmesh.devins.document.DocumentMetadata
import cc.unitmesh.devins.document.ParseStatus
import cc.unitmesh.devins.document.TOCItem
import cc.unitmesh.devins.document.Entity
import cc.unitmesh.devins.document.docql.executeDocQL
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cc.unitmesh.devins.ui.compose.agent.ResizableSplitPane
import cc.unitmesh.devins.ui.compose.agent.VerticalResizableSplitPane
import cc.unitmesh.devins.workspace.WorkspaceManager

/**
 * 文档阅读器主页面
 * 采用 3 栏布局：
 * 1. 左侧：文档导航 (DocumentNavigationPane)
 * 2. 中间：文档查看 (DocumentViewerPane) + 结构化信息 (StructuredInfoPane)
 * 3. 右侧：AI 聊天 (AIChatPane)
 */
@Composable
fun DocumentReaderPage(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {}
) {
    val workspace = remember { WorkspaceManager.getCurrentOrEmpty() }
    val viewModel = remember(workspace) { DocumentReaderViewModel(workspace) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            // TopAppBar is handled by the main layout usually, but if needed we can add one here
            // For now, we assume the main app has a top bar or we don't need one specific to this page
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            // 3-Pane Layout
            ResizableSplitPane(
                modifier = Modifier.fillMaxSize(),
                initialSplitRatio = 0.2f,
                minRatio = 0.15f,
                maxRatio = 0.3f,
                first = {
                    DocumentNavigationPane(
                        documents = viewModel.documents,
                        onDocumentSelected = { viewModel.selectDocument(it) }
                    )
                },
                second = {
                    ResizableSplitPane(
                        modifier = Modifier.fillMaxSize(),
                        initialSplitRatio = 0.7f,
                        minRatio = 0.5f,
                        maxRatio = 0.85f,
                        first = {
                            // 中间：文档查看 + 结构化信息
                            VerticalResizableSplitPane(
                                modifier = Modifier.fillMaxSize(),
                                initialSplitRatio = 0.6f,
                                minRatio = 0.3f,
                                maxRatio = 0.8f,
                                top = {
                                    // 中间上：文档查看器
                                    DocumentViewerPane(
                                        document = viewModel.selectedDocument,
                                        content = viewModel.documentContent,
                                        isLoading = viewModel.isLoading
                                    )
                                },
                                bottom = {
                                    // 中间下：结构化信息
                                    // TODO: Get TOC and Entities from ViewModel/Document
                                    val toc = viewModel.selectedDocument?.toc ?: emptyList()
                                    val entities = viewModel.selectedDocument?.entities ?: emptyList()

                                    StructuredInfoPane(
                                        toc = toc,
                                        entities = entities,
                                        onTocSelected = { /* TODO: Jump to anchor */ },
                                        onEntitySelected = { /* TODO: Show entity details */ },
                                        onDocQLQuery = { query ->
                                            // 执行 DocQL 查询
                                            val document = viewModel.selectedDocument
                                            if (document != null) {
                                                executeDocQL(query, document, null)
                                            } else {
                                                cc.unitmesh.devins.document.docql.DocQLResult.Error("没有选中的文档")
                                            }
                                        }
                                    )
                                }
                            )
                        },
                        second = {
                            // 右侧：AI 聊天
                            AIChatPane(
                                viewModel = viewModel
                            )
                        }
                    )
                }
            )
        }
    }
}
