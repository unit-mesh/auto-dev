package cc.unitmesh.devins.ui.compose.theme

import androidx.compose.ui.graphics.Color

/**
 * AutoDev Design System - Color Palette
 *
 * 基于色彩心理学设计的双主题（亮色/暗色）色彩系统
 *
 * 设计原则：
 * 1. 主色（Intelligent Indigo）- 融合蓝色的稳定和紫色的创造力
 * 2. 辅色（Spark Cyan）- AI 的"火花"，用于关键操作
 * 3. 暗黑模式避免纯黑纯白，使用去饱和化颜色
 */
object AutoDevColors {
    // ========================================================================
    // Indigo Scale - 主色调（智能靛蓝）
    // ========================================================================
    object Indigo {
        val c50 = Color(0xFFeef2ff)
        val c100 = Color(0xFFe0e7ff)
        val c200 = Color(0xFFc7d2fe)
        val c300 = Color(0xFFa5b4fc)  // 暗黑模式主色
        val c400 = Color(0xFF818cf8)  // 暗黑模式悬停
        val c500 = Color(0xFF6366f1)
        val c600 = Color(0xFF4f46e5)  // 亮色模式主色
        val c700 = Color(0xFF4338ca)  // 亮色模式悬停
        val c800 = Color(0xFF3730a3)
        val c900 = Color(0xFF312e81)
    }

    // ========================================================================
    // Cyan Scale - 辅助色（活力青色）
    // ========================================================================
    object Cyan {
        val c50 = Color(0xFFecfeff)
        val c100 = Color(0xFFcffafe)
        val c200 = Color(0xFFa5f3fc)
        val c300 = Color(0xFF67e8f9)
        val c400 = Color(0xFF22d3ee)  // 暗黑模式辅色
        val c500 = Color(0xFF06b6d4)  // 亮色模式辅色
        val c600 = Color(0xFF0891b2)
        val c700 = Color(0xFF0e7490)
        val c800 = Color(0xFF155e75)
        val c900 = Color(0xFF164e63)
    }

    // ========================================================================
    // Neutral Scale - 中性色（灰度）
    // ========================================================================
    object Neutral {
        val c50 = Color(0xFFfafafa)   // 亮色模式背景
        val c100 = Color(0xFFf5f5f5)  // 暗黑模式主文本
        val c200 = Color(0xFFe5e5e5)  // 亮色模式边框
        val c300 = Color(0xFFd4d4d4)  // 暗黑模式辅文本
        val c400 = Color(0xFFa3a3a3)
        val c500 = Color(0xFF737373)
        val c600 = Color(0xFF525252)
        val c700 = Color(0xFF404040)  // 暗黑模式边框
        val c800 = Color(0xFF262626)  // 暗黑模式卡片
        val c900 = Color(0xFF171717)  // 暗黑模式背景（避免纯黑）
    }

    // ========================================================================
    // Semantic Colors - 语义化颜色
    // ========================================================================
    object Green {
        val c50 = Color(0xFFf0fdf4)
        val c100 = Color(0xFFdcfce7)
        val c200 = Color(0xFFbbf7d0)
        val c300 = Color(0xFF86efac)  // 暗黑模式成功色
        val c400 = Color(0xFF4ade80)
        val c500 = Color(0xFF22c55e)
        val c600 = Color(0xFF16a34a)  // 亮色模式成功色
        val c700 = Color(0xFF15803d)
        val c800 = Color(0xFF166534)
        val c900 = Color(0xFF14532d)
    }

    object Amber {
        val c50 = Color(0xFFfffbeb)
        val c100 = Color(0xFFfef3c7)
        val c200 = Color(0xFFfde68a)
        val c300 = Color(0xFFfcd34d)  // 暗黑模式警告色
        val c400 = Color(0xFFfbbf24)
        val c500 = Color(0xFFf59e0b)  // 亮色模式警告色
        val c600 = Color(0xFFd97706)
        val c700 = Color(0xFFb45309)
        val c800 = Color(0xFF92400e)
        val c900 = Color(0xFF78350f)
    }

    object Red {
        val c50 = Color(0xFFfef2f2)
        val c100 = Color(0xFFfee2e2)
        val c200 = Color(0xFFfecaca)
        val c300 = Color(0xFFfca5a5)  // 暗黑模式错误色
        val c400 = Color(0xFFf87171)
        val c500 = Color(0xFFef4444)
        val c600 = Color(0xFFdc2626)  // 亮色模式错误色
        val c700 = Color(0xFFb91c1c)
        val c800 = Color(0xFF991b1b)
        val c900 = Color(0xFF7f1d1d)
    }

    object Blue {
        val c50 = Color(0xFFeff6ff)
        val c100 = Color(0xFFdbeafe)
        val c200 = Color(0xFFbfdbfe)
        val c300 = Color(0xFF93c5fd)  // 暗黑模式信息色
        val c400 = Color(0xFF60a5fa)
        val c500 = Color(0xFF3b82f6)  // 亮色模式信息色
        val c600 = Color(0xFF2563eb)
        val c700 = Color(0xFF1d4ed8)
        val c800 = Color(0xFF1e40af)
        val c900 = Color(0xFF1e3a8a)
    }

    // ========================================================================
    // Syntax Highlighting Colors - 代码高亮专用颜色
    // ========================================================================
    object Syntax {
        // 深色主题代码高亮
        object Dark {
            val agent = Cyan.c400        // Agent 提及 (@)
            val command = Green.c400     // 命令 (/)
            val variable = Color(0xFFFF6EC7)  // 变量 ($) - 洋红
            val keyword = Color(0xFFCC7832)   // 关键字 - 橙色
            val string = Green.c600      // 字符串
            val number = Blue.c400       // 数字
            val comment = Neutral.c500   // 注释
            val identifier = Neutral.c300 // 标识符
        }

        // 亮色主题代码高亮
        object Light {
            val agent = Cyan.c600        // Agent 提及 (@)
            val command = Green.c600     // 命令 (/)
            val variable = Color(0xFFD946C0)  // 变量 ($) - 洋红
            val keyword = Color(0xFFB4570E)   // 关键字 - 橙色
            val string = Green.c700      // 字符串
            val number = Blue.c600       // 数字
            val comment = Neutral.c500   // 注释
            val identifier = Neutral.c700 // 标识符
        }
    }

    // ========================================================================
    // Diff Colors - Diff 显示专用颜色
    // ========================================================================
    object Diff {
        // 深色主题
        object Dark {
            val addedBg = Green.c300.copy(alpha = 0.15f)
            val addedBorder = Green.c300.copy(alpha = 0.3f)
            val deletedBg = Red.c300.copy(alpha = 0.15f)
            val deletedBorder = Red.c300.copy(alpha = 0.3f)
            val lineNumber = Neutral.c500
        }

        // 亮色主题
        object Light {
            val addedBg = Green.c600.copy(alpha = 0.1f)
            val addedBorder = Green.c600.copy(alpha = 0.3f)
            val deletedBg = Red.c600.copy(alpha = 0.1f)
            val deletedBorder = Red.c600.copy(alpha = 0.3f)
            val lineNumber = Neutral.c600
        }
    }
}

