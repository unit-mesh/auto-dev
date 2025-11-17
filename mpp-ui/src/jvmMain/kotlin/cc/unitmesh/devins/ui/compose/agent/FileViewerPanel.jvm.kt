package cc.unitmesh.devins.ui.compose.agent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cc.unitmesh.devins.ui.compose.icons.AutoDevComposeIcons
import cc.unitmesh.viewer.LanguageDetector
import cc.unitmesh.viewer.ViewerRequest
import cc.unitmesh.viewer.ViewerType
import cc.unitmesh.viewer.web.ViewerWebView
import cc.unitmesh.viewer.web.createWebViewerHost
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun FileViewerPanel(
    filePath: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var fileName by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var fileContent by remember { mutableStateOf<String?>(null) }

    // Viewer host - will be initialized by ViewerWebView
    var viewerHost by remember { mutableStateOf<cc.unitmesh.viewer.ViewerHost?>(null) }

    // 异步加载文件内容
    LaunchedEffect(filePath) {
        println("[FileViewerPanel] Starting to load file: $filePath")
        isLoading = true
        errorMessage = null
        fileContent = null

        withContext(Dispatchers.IO) {
            try {
                val file = File(filePath)
                if (!file.exists()) {
                    println("[FileViewerPanel] ERROR: File not found: $filePath")
                    errorMessage = "File not found: $filePath"
                    fileName = filePath
                    return@withContext
                }

                fileName = file.name
                println("[FileViewerPanel] File exists: ${file.name}, size: ${file.length()} bytes")

                // 检查是否是二进制文件
                if (isBinaryFile(file)) {
                    println("[FileViewerPanel] ERROR: Binary file detected: ${file.name}")
                    errorMessage = "Cannot open binary file: ${file.name}"
                    return@withContext
                }

                // 检查文件大小（限制 10MB）
                val maxSize = 10 * 1024 * 1024 // 10MB
                if (file.length() > maxSize) {
                    println("[FileViewerPanel] ERROR: File too large: ${file.length() / 1024 / 1024}MB")
                    errorMessage = "File too large (${file.length() / 1024 / 1024}MB). Maximum size is 10MB."
                    return@withContext
                }

                // 异步读取文件
                try {
                    fileContent = file.readText()
                    println("[FileViewerPanel] SUCCESS: File content loaded, length: ${fileContent?.length} chars")
                } catch (e: java.nio.charset.MalformedInputException) {
                    println("[FileViewerPanel] ERROR: Cannot decode file: ${e.message}")
                    errorMessage = "Cannot decode file (likely binary): ${file.name}"
                }
            } catch (e: Exception) {
                println("[FileViewerPanel] ERROR: Exception loading file: ${e.message}")
                e.printStackTrace()
                errorMessage = "Error loading file: ${e.message}"
                fileName = filePath
            } finally {
                isLoading = false
                println("[FileViewerPanel] Loading finished. isLoading=false, errorMessage=$errorMessage, fileContent length=${fileContent?.length}")
            }
        }
    }

    // Prepare viewer request when file is loaded
    val viewerRequest = remember(fileContent) {
        if (fileContent != null) {
            val file = File(filePath)
            val language = LanguageDetector.detectLanguage(filePath)

            val request = ViewerRequest(
                type = ViewerType.CODE,
                content = fileContent!!,
                language = language,
                fileName = file.name,
                filePath = filePath,
                readOnly = true
            )
            println("[FileViewerPanel] ViewerRequest created: type=${request.type}, language=${request.language}, contentLength=${request.content.length}")
            request
        } else {
            println("[FileViewerPanel] ViewerRequest is null (fileContent is null)")
            null
        }
    }

    Column(
        modifier =
            modifier
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surface)
    ) {
        // Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = fileName,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = filePath,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = AutoDevComposeIcons.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Content
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                isLoading -> {
                    println("[FileViewerPanel] UI: Showing loading state")
                    // Loading state
                    Column(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Loading file...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                errorMessage != null -> {
                    println("[FileViewerPanel] UI: Showing error state: $errorMessage")
                    // Error state
                    Column(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Error",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                fileContent != null && viewerRequest != null -> {
                    println("[FileViewerPanel] UI: Showing WebView (fileContent=${fileContent?.length} chars, viewerRequest=$viewerRequest)")
                    // WebView content with Monaco Editor
                    ViewerWebView(
                        initialRequest = viewerRequest,
                        modifier = Modifier.fillMaxSize(),
                        onHostCreated = { host ->
                            println("[FileViewerPanel] WebViewerHost created and assigned")
                            viewerHost = host
                        }
                    )
                }
                else -> {
                    println("[FileViewerPanel] UI: No matching state! isLoading=$isLoading, errorMessage=$errorMessage, fileContent=${fileContent?.length}, viewerRequest=$viewerRequest")
                }
            }
        }
    }
}

/**
 * Check if a file is likely a binary file
 */
private fun isBinaryFile(file: File): Boolean {
    val extension = file.extension.lowercase()

    // 常见的二进制文件扩展名
    val binaryExtensions =
        setOf(
            // 编译产物
            "class", "jar", "war", "ear", "zip", "tar", "gz", "bz2", "7z", "rar",
            // 可执行文件
            "exe", "dll", "so", "dylib", "bin", "app",
            // 图片
            "png", "jpg", "jpeg", "gif", "bmp", "ico", "webp", "tiff",
            // 视频
            "mp4", "avi", "mov", "mkv", "wmv", "flv", "webm",
            // 音频
            "mp3", "wav", "ogg", "flac", "aac", "wma",
            // 字体
            "ttf", "otf", "woff", "woff2", "eot",
            // 数据库
            "db", "sqlite", "sqlite3",
            // 其他
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx"
        )

    if (extension in binaryExtensions) {
        return true
    }

    // 对于无扩展名或未知扩展名的文件，检查前 512 字节是否包含二进制字符
    if (file.length() > 0) {
        try {
            val bytesToCheck = minOf(512, file.length().toInt())
            val bytes = file.inputStream().use { it.readNBytes(bytesToCheck) }

            // 检查是否包含 NULL 字节或其他控制字符（除了常见的如 \n, \r, \t）
            val binaryThreshold = 0.3 // 如果超过 30% 是非文本字符，认为是二进制
            var nonTextCount = 0

            for (byte in bytes) {
                val b = byte.toInt() and 0xFF
                // NULL 字节或其他控制字符（除了 \n=10, \r=13, \t=9）
                if (b == 0 || (b < 32 && b != 9 && b != 10 && b != 13)) {
                    nonTextCount++
                }
            }

            return (nonTextCount.toDouble() / bytes.size) > binaryThreshold
        } catch (e: Exception) {
            // 读取失败，保守起见认为是二进制
            return true
        }
    }

    return false
}
