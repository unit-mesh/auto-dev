package cc.unitmesh.devins.ui.compose.document

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import cc.unitmesh.devins.document.docql.executeDocQL
import cc.unitmesh.devins.ui.compose.agent.ResizableSplitPane
import cc.unitmesh.devins.ui.compose.agent.VerticalResizableSplitPane
import cc.unitmesh.devins.workspace.WorkspaceManager

/**
 * 文档阅读器主页面
 * 采用查询优先的 3 栏布局：
 * 1. 左侧：AI 聊天 (DocumentChatPane) - 主要交互区域，显示索引状态
 * 2. 中间：文档导航 (DocumentNavigationPane) - 文件列表
 * 3. 右侧：文档查看 (DocumentViewerPane) + 结构化信息 (StructuredInfoPane) - 垂直分割
 *
 * 工作流：用户在左侧查询 → 中间看到文件列表 → 右侧查看详细内容和结构
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
            ResizableSplitPane(
                modifier = Modifier.fillMaxSize(),
                initialSplitRatio = 0.65f,
                minRatio = 0.35f,
                maxRatio = 0.85f,
                first = {
                    DocumentChatPane(
                        viewModel = viewModel
                    )
                },
                second = {
                    ResizableSplitPane(
                        modifier = Modifier.fillMaxSize(),
                        initialSplitRatio = 0.5f,
                        minRatio = 0.3f,
                        maxRatio = 0.8f,
                        first = {
                            DocumentNavigationPane(
                                documentLoadState = viewModel.documentLoadState,
                                documents = viewModel.filteredDocuments,
                                indexingStatus = viewModel.indexingStatus.collectAsState().value,
                                searchQuery = viewModel.searchQuery,
                                onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                                onDocumentSelected = { viewModel.selectDocument(it) },
                                onRefresh = { viewModel.refreshDocuments() },
                                onStartIndexing = { viewModel.startIndexing() },
                                onResetIndexing = { viewModel.resetIndexingStatus() }
                            )
                        },
                        second = {
                            // Conditional rendering: 
                            // - When no document is selected, show StructuredInfoPane globally
                            // - When a document is selected, show DocumentViewerPane with StructuredInfoPane below
                            if (viewModel.selectedDocument == null) {
                                // No document selected - show global StructuredInfoPane
                                StructuredInfoPane(
                                    toc = emptyList(),
                                    entities = emptyList(),
                                    onTocSelected = { /* No action when no document selected */ },
                                    onEntitySelected = { /* No action when no document selected */ },
                                    onDocQLQuery = { query ->
                                        // Global query across all indexed documents
                                        try {
                                            cc.unitmesh.devins.document.DocumentRegistry.queryDocuments(query)
                                        } catch (e: Exception) {
                                            cc.unitmesh.devins.document.docql.DocQLResult.Error("全局查询失败: ${e.message}")
                                        }
                                    }
                                )
                            } else {
                                // Document selected - show DocumentViewerPane with StructuredInfoPane
                                VerticalResizableSplitPane(
                                    modifier = Modifier.fillMaxSize(),
                                    initialSplitRatio = 0.5f,
                                    minRatio = 0.2f,
                                    maxRatio = 0.8f,
                                    top = {
                                        DocumentViewerPane(
                                            document = viewModel.selectedDocument,
                                            rawContent = viewModel.documentContent,
                                            parsedContent = viewModel.parsedContent,
                                            isLoading = viewModel.isLoading,
                                            indexStatus = viewModel.selectedDocumentIndexStatus,
                                            targetLineNumber = viewModel.targetLineNumber,
                                            highlightedText = viewModel.highlightedText
                                        )
                                    },
                                    bottom = {
                                        val toc = viewModel.selectedDocument?.toc ?: emptyList()
                                        val entities = viewModel.selectedDocument?.entities ?: emptyList()

                                        StructuredInfoPane(
                                            toc = toc,
                                            entities = entities,
                                            onTocSelected = { tocItem ->
                                                viewModel.navigateToTocItem(tocItem)
                                            },
                                            onEntitySelected = { entity ->
                                                viewModel.navigateToEntity(entity)
                                            },
                                            onDocQLQuery = { query ->
                                                val document = viewModel.selectedDocument
                                                if (document != null) {
                                                    // 查询当前选中的文档
                                                    executeDocQL(query, document, null)
                                                } else {
                                                    // 全局查询所有已索引的文档
                                                    try {
                                                        cc.unitmesh.devins.document.DocumentRegistry.queryDocuments(query)
                                                    } catch (e: Exception) {
                                                        cc.unitmesh.devins.document.docql.DocQLResult.Error("全局查询失败: ${e.message}")
                                                    }
                                                }
                                            }
                                        )
                                    }
                                )
                            }
                        }
                    )
                }
            )
        }
    }
}
