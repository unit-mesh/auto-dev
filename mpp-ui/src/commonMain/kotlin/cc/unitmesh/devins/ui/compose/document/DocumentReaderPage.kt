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
            ResizableSplitPane(
                modifier = Modifier.fillMaxSize(),
                initialSplitRatio = 0.18f,
                minRatio = 0.12f,
                maxRatio = 0.2f,
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
                    ResizableSplitPane(
                        modifier = Modifier.fillMaxSize(),
                        initialSplitRatio = 0.45f,
                        minRatio = 0.35f,
                        maxRatio = 0.5f,
                        first = {
                            VerticalResizableSplitPane(
                                modifier = Modifier.fillMaxSize(),
                                initialSplitRatio = 0.5f,
                                minRatio = 0.4f,
                                maxRatio = 0.85f,
                                top = {
                                    DocumentViewerPane(
                                        document = viewModel.selectedDocument,
                                        content = viewModel.documentContent,
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
                            DocumentChatPane(
                                viewModel = viewModel
                            )
                        }
                    )
                }
            )
        }
    }
}
