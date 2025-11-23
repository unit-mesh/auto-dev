package cc.unitmesh.devins.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import cc.unitmesh.devins.ui.compose.agent.ResizableSplitPane
import cc.unitmesh.devins.ui.compose.agent.VerticalResizableSplitPane
import cc.unitmesh.devins.ui.compose.document.*
import cc.unitmesh.devins.document.docql.executeDocQL
import cc.unitmesh.devins.workspace.DefaultWorkspace
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Standalone JVM main for testing Document Reader
 *
 * Usage: Run this main function to test document reading with the specified Markdown file
 */
fun main() = application {
    val windowState = rememberWindowState(width = 1400.dp, height = 900.dp)

    Window(
        onCloseRequest = ::exitApplication,
        title = "AutoDev Document Reader - Test",
        state = windowState
    ) {
        MaterialTheme {
            Surface(modifier = Modifier.fillMaxSize()) {
                DocumentReaderTestApp()
            }
        }
    }
}

@Composable
fun DocumentReaderTestApp() {
    val workspace = remember {
        // Create workspace with project root so fileSystem can read files
        DefaultWorkspace.create(
            name = "Test Workspace",
            rootPath = "/Volumes/source/ai/autocrud"
        )
    }

    val viewModel = remember(workspace) {
        DocumentReaderViewModel(workspace)
    }

    // Load test file when the composable is first created
    LaunchedEffect(Unit) {
        viewModel.loadDocumentFromPath("/Volumes/source/ai/autocrud/README.md")
    }

    DocumentReaderPageWithViewModel(
        viewModel = viewModel,
        modifier = Modifier.fillMaxSize()
    )
}

/**
 * Document Reader Page with custom ViewModel
 */
@Composable
private fun DocumentReaderPageWithViewModel(
    viewModel: DocumentReaderViewModel,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            ResizableSplitPane(
                modifier = Modifier.fillMaxSize(),
                initialSplitRatio = 0.2f,
                minRatio = 0.15f,
                maxRatio = 0.3f,
                first = {
                    // Left: Document Navigation
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
                            // Middle: Document Viewer + Structured Info
                            VerticalResizableSplitPane(
                                modifier = Modifier.fillMaxSize(),
                                initialSplitRatio = 0.6f,
                                minRatio = 0.3f,
                                maxRatio = 0.8f,
                                top = {
                                    // Middle Top: Document Viewer
                                    DocumentViewerPane(
                                        document = viewModel.selectedDocument,
                                        content = viewModel.documentContent,
                                        isLoading = viewModel.isLoading
                                    )
                                },
                                bottom = {
                                    // Middle Bottom: Structured Info
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
                                                executeDocQL(query, document, viewModel.getParserService())
                                            } else {
                                                cc.unitmesh.devins.document.docql.DocQLResult.Error("没有选中的文档")
                                            }
                                        }
                                    )
                                }
                            )
                        },
                        second = {
                            // Right: AI Chat
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
