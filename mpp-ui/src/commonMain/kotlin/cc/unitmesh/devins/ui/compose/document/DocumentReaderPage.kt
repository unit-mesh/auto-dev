package cc.unitmesh.devins.ui.compose.document

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cc.unitmesh.devins.ui.compose.icons.AutoDevComposeIcons
import cc.unitmesh.llm.KoogLLMService

/**
 * 文档阅读器页面 - AI 原生文档阅读界面
 * 
 * 采用 3 栏布局：
 * - 左侧：文档导航
 * - 中间上：文档查看器
 * - 中间下：结构化信息（TOC/Graph/Entities）
 * - 右侧：AI 聊天
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentReaderPage(
    llmService: KoogLLMService?,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {}
) {
    // 状态管理 (临时)
    var selectedDocument by remember { mutableStateOf<DocumentFile?>(null) }
    var documentContent by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    
    // 模拟数据
    val dummyDocuments = remember { createDummyDocuments() }
    val dummyToc = remember { createDummyToc() }
    val dummyEntities = remember { createDummyEntities() }
    val messages = remember { mutableStateListOf<cc.unitmesh.devins.llm.Message>() }
    var isGenerating by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "智能文档",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "AI-Native Document Reader",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = AutoDevComposeIcons.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 3栏布局
            cc.unitmesh.devins.ui.compose.agent.ResizableSplitPane(
                modifier = Modifier.fillMaxSize(),
                initialSplitRatio = 0.2f,
                minRatio = 0.15f,
                maxRatio = 0.3f,
                first = {
                    // 左侧：文档导航
                    DocumentNavigationPane(
                        documents = dummyDocuments,
                        onDocumentSelected = { doc ->
                            selectedDocument = doc
                            isLoading = true
                            // 模拟加载
                            documentContent = "# ${doc.name}\n\nThis is a placeholder content for **${doc.name}**.\n\n## Introduction\n\nAI-native document reading is the future."
                            isLoading = false
                        }
                    )
                },
                second = {
                    cc.unitmesh.devins.ui.compose.agent.ResizableSplitPane(
                        modifier = Modifier.fillMaxSize(),
                        initialSplitRatio = 0.7f,
                        minRatio = 0.5f,
                        maxRatio = 0.8f,
                        first = {
                            // 中间：文档查看 + 结构化信息
                            cc.unitmesh.devins.ui.compose.agent.VerticalResizableSplitPane(
                                modifier = Modifier.fillMaxSize(),
                                initialSplitRatio = 0.6f,
                                minRatio = 0.3f,
                                maxRatio = 0.8f,
                                top = {
                                    // 中间上：文档查看器
                                    DocumentViewerPane(
                                        document = selectedDocument,
                                        content = documentContent,
                                        isLoading = isLoading
                                    )
                                },
                                bottom = {
                                    // 中间下：结构化信息
                                    StructuredInfoPane(
                                        toc = dummyToc,
                                        entities = dummyEntities,
                                        onTocSelected = { /* TODO: Jump to anchor */ },
                                        onEntitySelected = { /* TODO: Show entity details */ }
                                    )
                                }
                            )
                        },
                        second = {
                            // 右侧：AI 聊天
                            AIChatPane(
                                messages = messages,
                                isGenerating = isGenerating,
                                onSendMessage = { text ->
                                    messages.add(cc.unitmesh.devins.llm.Message(cc.unitmesh.devins.llm.MessageRole.USER, text))
                                    isGenerating = true
                                    // 模拟 AI 回复
                                    // In real implementation, this would call ViewModel/LLMService
                                },
                                onStopGeneration = { isGenerating = false }
                            )
                        }
                    )
                }
            )
        }
    }
}

// --- Dummy Data Generators ---

private fun createDummyDocuments(): List<DocumentFile> {
    val meta = DocumentMetadata(10, 5, ParseStatus.PARSED, 1678888888000L, 10240, "Markdown", "text/markdown")
    return listOf(
        DocumentFile("README.md", "README.md", meta),
        DocumentFile("Architecture.md", "docs/Architecture.md", meta),
        DocumentFile("API.md", "docs/API.md", meta)
    )
}

private fun createDummyToc(): List<TOCItem> {
    return listOf(
        TOCItem(1, "Introduction", "#intro", 1, 1, emptyList()),
        TOCItem(1, "Architecture", "#arch", 2, 10, listOf(
            TOCItem(2, "Components", "#comp", 2, 15, emptyList()),
            TOCItem(2, "Data Flow", "#flow", 3, 25, emptyList())
        )),
        TOCItem(1, "API Reference", "#api", 5, 50, emptyList())
    )
}

private fun createDummyEntities(): List<Entity> {
    return listOf(
        Entity.Term("RAG", "Retrieval-Augmented Generation", Location("0", 0, 0)),
        Entity.ClassEntity("DocumentReader", "cc.unitmesh.reader", Location("0", 0, 0)),
        Entity.FunctionEntity("parseDocument", "fun parseDocument(file: File)", Location("0", 0, 0))
    )
}
