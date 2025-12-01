package cc.unitmesh.devins.idea.editor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import cc.unitmesh.llm.KoogLLMService
import cc.unitmesh.llm.PromptEnhancer
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.*

/**
 * Prompt Optimization Dialog for IntelliJ IDEA.
 * 
 * Features:
 * - Display original and enhanced prompts side by side
 * - Real-time enhancement using PromptEnhancer
 * - Apply or cancel the enhancement
 * 
 * Migrated from mpp-ui/DevInEditorInput.kt prompt enhancement functionality.
 */
@Composable
fun IdeaPromptOptimizationDialog(
    originalText: String,
    enhancer: PromptEnhancer?,
    onApply: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var enhancedText by remember { mutableStateOf("") }
    var isEnhancing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val scope = rememberCoroutineScope()
    
    // Auto-enhance on dialog open
    LaunchedEffect(Unit) {
        if (enhancer != null && originalText.isNotBlank()) {
            isEnhancing = true
            errorMessage = null
            try {
                val enhanced = enhancer.enhance(originalText.trim(), "zh")
                if (enhanced.isNotEmpty() && enhanced != originalText.trim()) {
                    enhancedText = enhanced
                } else {
                    enhancedText = originalText
                    errorMessage = "No enhancement needed or enhancement failed"
                }
            } catch (e: Exception) {
                errorMessage = "Enhancement failed: ${e.message}"
                enhancedText = originalText
            } finally {
                isEnhancing = false
            }
        } else {
            enhancedText = originalText
            if (enhancer == null) {
                errorMessage = "Enhancer not available. Please configure LLM settings."
            }
        }
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .width(700.dp)
                .height(500.dp)
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Prompt Optimization (Ctrl+P)")
                IconButton(onClick = onDismiss) {
                    Text("×")
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Error message
            errorMessage?.let { error ->
                Text(error, color = JewelTheme.globalColors.text.error)
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Content area
            Row(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Original text
                Column(modifier = Modifier.weight(1f)) {
                    Text("Original")
                    Spacer(modifier = Modifier.height(4.dp))
                    BasicTextField(
                        value = originalText,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxSize(),
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            color = JewelTheme.globalColors.text.normal
                        ),
                        cursorBrush = SolidColor(JewelTheme.globalColors.text.normal)
                    )
                }
                
                // Enhanced text
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Enhanced")
                        if (isEnhancing) {
                            Text("(Enhancing...)", color = JewelTheme.globalColors.text.info)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    val enhancedTextState = rememberTextFieldState(enhancedText)
                    
                    LaunchedEffect(enhancedText) {
                        if (enhancedTextState.text.toString() != enhancedText) {
                            enhancedTextState.setTextAndPlaceCursorAtEnd(enhancedText)
                        }
                    }
                    
                    LaunchedEffect(Unit) {
                        snapshotFlow { enhancedTextState.text.toString() }
                            .distinctUntilChanged()
                            .collect { newText ->
                                if (newText != enhancedText) {
                                    enhancedText = newText
                                }
                            }
                    }
                    
                    BasicTextField(
                        state = enhancedTextState,
                        modifier = Modifier.fillMaxSize(),
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            color = JewelTheme.globalColors.text.normal
                        ),
                        cursorBrush = SolidColor(JewelTheme.globalColors.text.normal)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Footer buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = onDismiss) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.width(8.dp))
                DefaultButton(
                    onClick = { onApply(enhancedText) },
                    enabled = !isEnhancing && enhancedText.isNotBlank()
                ) {
                    Text("Apply")
                }
            }
        }
    }
}

