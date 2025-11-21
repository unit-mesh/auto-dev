package cc.unitmesh.devins.ui.desktop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
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
 * - File 菜单：打开项目 (Cmd+O on Mac / Ctrl+O on others)、退出
 * - View 菜单：语言切换、主题切换
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
                shortcut = Keymap.openProject,
                mnemonic = 'O'
            )
            
            Separator()
            
            Item(
                "Exit",
                onClick = onExit,
                shortcut = Keymap.exitApp,
                mnemonic = 'x'
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
                    "Light",
                    onClick = {
                        ThemeManager.setTheme(ThemeManager.ThemeMode.LIGHT)
                    },
                    mnemonic = 'L'
                )
                
                Item(
                    "Dark",
                    onClick = {
                        ThemeManager.setTheme(ThemeManager.ThemeMode.DARK)
                    },
                    mnemonic = 'D'
                )
                
                Item(
                    "Auto (System)",
                    onClick = {
                        ThemeManager.setTheme(ThemeManager.ThemeMode.SYSTEM)
                    },
                    mnemonic = 'A'
                )
            }
        }
    }
}

