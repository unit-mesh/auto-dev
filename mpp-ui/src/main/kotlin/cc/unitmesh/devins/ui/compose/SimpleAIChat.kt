package cc.unitmesh.devins.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cc.unitmesh.devins.ui.compose.editor.DevInEditorInput
import cc.unitmesh.devins.ui.compose.editor.model.EditorCallbacks

/**
 * 简洁 AI 聊天界面
 * 顶部输入框 + 底部工具栏
 */
@Composable
fun SimpleAIChat() {
    val callbacks = object : EditorCallbacks {
        override fun onSubmit(text: String) {
            println("✅ 提交内容:")
            println(text)
            println("\n📝 解析结果:")
            println(analyzeDevInInput(text))
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        // 完整的输入组件（包含底部工具栏）
        DevInEditorInput(
            initialText = "",
            placeholder = "Plan, @ for context, / for commands",
            callbacks = callbacks,
            modifier = Modifier
                .fillMaxWidth(0.9f) // 90% 宽度，更居中
        )
    }
}

/**
 * 分析 DevIn 输入
 */
private fun analyzeDevInInput(text: String): String {
    val analysis = mutableListOf<String>()
    
    // 检测 Agent
    val agents = Regex("@(\\w+)").findAll(text).map { it.groupValues[1] }.toList()
    if (agents.isNotEmpty()) {
        analysis.add("检测到 Agents: ${agents.joinToString(", ")}")
    }
    
    // 检测 Command
    val commands = Regex("/(\\w+):").findAll(text).map { it.groupValues[1] }.toList()
    if (commands.isNotEmpty()) {
        analysis.add("检测到 Commands: ${commands.joinToString(", ")}")
    }
    
    // 检测 Variable
    val variables = Regex("\\$(\\w+)").findAll(text).map { it.groupValues[1] }.toList()
    if (variables.isNotEmpty()) {
        analysis.add("检测到 Variables: ${variables.joinToString(", ")}")
    }
    
    // 检测 FrontMatter
    if (text.contains("---")) {
        analysis.add("包含 FrontMatter 配置")
    }
    
    // 检测代码块
    val codeBlocks = Regex("```(\\w*)").findAll(text).map { it.groupValues[1].ifEmpty { "plain" } }.toList()
    if (codeBlocks.isNotEmpty()) {
        analysis.add("包含代码块: ${codeBlocks.joinToString(", ")}")
    }
    
    return if (analysis.isNotEmpty()) {
        analysis.joinToString("\n• ", "• ")
    } else {
        "纯文本输入"
    }
}

private fun getExamplePrompt(): String = ""

