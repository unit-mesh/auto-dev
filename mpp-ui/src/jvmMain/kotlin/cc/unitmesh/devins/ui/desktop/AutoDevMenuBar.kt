package cc.unitmesh.devins.ui.desktop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.MenuBar
import cc.unitmesh.devins.ui.compose.theme.ThemeManager
import cc.unitmesh.devins.ui.i18n.Language
import cc.unitmesh.devins.ui.i18n.LanguageManager
import kotlinx.coroutines.launch

/**
 * AutoDev Desktop 菜单栏
 * 
 * 提供常用的桌面应用菜单功能：
 * - File 菜单：打开项目 (Ctrl+O)、退出
 * - Edit 菜单：复制、粘贴等（未来扩展）
 * - View 菜单：语言切换、主题切换
 * - Help 菜单：关于、文档等（未来扩展）
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun FrameWindowScope.AutoDevMenuBar(
    onOpenFile: () -> Unit,
    onExit: () -> Unit
) {
    val scope = rememberCoroutineScope()
    
    MenuBar {
        // File 菜单
        Menu("File", mnemonic = 'F') {
            Item(
                "Open Project...",
                onClick = onOpenFile,
                shortcut = KeyShortcut(Key.O, ctrl = true),
                mnemonic = 'O'
            )
            
            Separator()
            
            Item(
                "Exit",
                onClick = onExit,
                shortcut = KeyShortcut(Key.Q, ctrl = true),
                mnemonic = 'x'
            )
        }
        
        // Edit 菜单（未来扩展）
        Menu("Edit", mnemonic = 'E') {
            Item(
                "Copy",
                onClick = { /* TODO: 实现复制功能 */ },
                shortcut = KeyShortcut(Key.C, ctrl = true),
                mnemonic = 'C',
                enabled = false
            )
            
            Item(
                "Paste",
                onClick = { /* TODO: 实现粘贴功能 */ },
                shortcut = KeyShortcut(Key.V, ctrl = true),
                mnemonic = 'P',
                enabled = false
            )
        }
        
        // View 菜单（Language & Theme）
        Menu("View", mnemonic = 'V') {
            // Language submenu
            Menu("Language") {
                Item(
                    "English",
                    onClick = {
                        scope.launch {
                            LanguageManager.setLanguage(Language.ENGLISH)
                        }
                    },
                    mnemonic = 'E'
                )
                
                Item(
                    "中文",
                    onClick = {
                        scope.launch {
                            LanguageManager.setLanguage(Language.CHINESE)
                        }
                    },
                    mnemonic = 'Z'
                )
            }
            
            Separator()
            
            // Theme submenu
            Menu("Theme") {
                Item(
                    "☀\uFE0F Light",
                    onClick = {
                        ThemeManager.setTheme(ThemeManager.ThemeMode.LIGHT)
                    },
                    mnemonic = 'L'
                )
                
                Item(
                    "\uD83C\uDF19 Dark",
                    onClick = {
                        ThemeManager.setTheme(ThemeManager.ThemeMode.DARK)
                    },
                    mnemonic = 'D'
                )
                
                Item(
                    "\uD83D\uDD06 Auto (System)",
                    onClick = {
                        ThemeManager.setTheme(ThemeManager.ThemeMode.SYSTEM)
                    },
                    mnemonic = 'A'
                )
            }
        }
        
        // Help 菜单（未来扩展）
        Menu("Help", mnemonic = 'H') {
            Item(
                "Documentation",
                onClick = { /* TODO: 打开文档 */ },
                mnemonic = 'D'
            )
            
            Separator()
            
            Item(
                "About AutoDev",
                onClick = { /* TODO: 显示关于对话框 */ },
                mnemonic = 'A'
            )
        }
    }
}

