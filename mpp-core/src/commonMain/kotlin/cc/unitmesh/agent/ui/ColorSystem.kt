package cc.unitmesh.agent.ui

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

/**
 * 色阶定义
 */
data class ColorScale(
    val c50: String,
    val c100: String,
    val c200: String,
    val c300: String,
    val c400: String,
    val c500: String,
    val c600: String,
    val c700: String,
    val c800: String,
    val c900: String
)

/**
 * Indigo Scale - 主色调（智能靛蓝）
 */
object IndigoScale {
    const val c50 = "#eef2ff"
    const val c100 = "#e0e7ff"
    const val c200 = "#c7d2fe"
    const val c300 = "#a5b4fc"  // 暗黑模式主色
    const val c400 = "#818cf8"  // 暗黑模式悬停
    const val c500 = "#6366f1"
    const val c600 = "#4f46e5"  // 亮色模式主色
    const val c700 = "#4338ca"  // 亮色模式悬停
    const val c800 = "#3730a3"
    const val c900 = "#312e81"
}

/**
 * Cyan Scale - 辅助色（活力青色）
 */
object CyanScale {
    const val c50 = "#ecfeff"
    const val c100 = "#cffafe"
    const val c200 = "#a5f3fc"
    const val c300 = "#67e8f9"
    const val c400 = "#22d3ee"  // 暗黑模式辅色
    const val c500 = "#06b6d4"  // 亮色模式辅色
    const val c600 = "#0891b2"
    const val c700 = "#0e7490"
    const val c800 = "#155e75"
    const val c900 = "#164e63"
}

/**
 * Neutral Scale - 中性色（灰度）
 */
object NeutralScale {
    const val c50 = "#fafafa"   // 亮色模式背景
    const val c100 = "#f5f5f5"  // 暗黑模式主文本
    const val c200 = "#e5e5e5"  // 亮色模式边框
    const val c300 = "#d4d4d4"  // 暗黑模式辅文本
    const val c400 = "#a3a3a3"
    const val c500 = "#737373"
    const val c600 = "#525252"
    const val c700 = "#404040"  // 暗黑模式边框
    const val c800 = "#262626"  // 暗黑模式卡片
    const val c900 = "#171717"  // 暗黑模式背景（避免纯黑）
}

/**
 * Green Scale - 成功状态
 */
object GreenScale {
    const val c50 = "#f0fdf4"
    const val c100 = "#dcfce7"
    const val c200 = "#bbf7d0"
    const val c300 = "#86efac"  // 暗黑模式成功色
    const val c400 = "#4ade80"
    const val c500 = "#22c55e"
    const val c600 = "#16a34a"  // 亮色模式成功色
    const val c700 = "#15803d"
    const val c800 = "#166534"
    const val c900 = "#14532d"
}

/**
 * Amber Scale - 警告状态
 */
object AmberScale {
    const val c50 = "#fffbeb"
    const val c100 = "#fef3c7"
    const val c200 = "#fde68a"
    const val c300 = "#fcd34d"  // 暗黑模式警告色
    const val c400 = "#fbbf24"
    const val c500 = "#f59e0b"  // 亮色模式警告色
    const val c600 = "#d97706"
    const val c700 = "#b45309"
    const val c800 = "#92400e"
    const val c900 = "#78350f"
}

/**
 * Red Scale - 错误状态
 */
object RedScale {
    const val c50 = "#fef2f2"
    const val c100 = "#fee2e2"
    const val c200 = "#fecaca"
    const val c300 = "#fca5a5"  // 暗黑模式错误色
    const val c400 = "#f87171"
    const val c500 = "#ef4444"
    const val c600 = "#dc2626"  // 亮色模式错误色
    const val c700 = "#b91c1c"
    const val c800 = "#991b1b"
    const val c900 = "#7f1d1d"
}

/**
 * Blue Scale - 信息状态
 */
object BlueScale {
    const val c50 = "#eff6ff"
    const val c100 = "#dbeafe"
    const val c200 = "#bfdbfe"
    const val c300 = "#93c5fd"  // 暗黑模式信息色
    const val c400 = "#60a5fa"
    const val c500 = "#3b82f6"  // 亮色模式信息色
    const val c600 = "#2563eb"
    const val c700 = "#1d4ed8"
    const val c800 = "#1e40af"
    const val c900 = "#1e3a8a"
}

/**
 * 主题色彩令牌
 */
data class ColorTheme(
    // Primary Colors
    val primary: String,
    val primaryHover: String,
    val primaryActive: String,
    
    // Accent Colors
    val accent: String,
    val accentHover: String,
    
    // Text Colors
    val textPrimary: String,
    val textSecondary: String,
    val textTertiary: String,
    val textInverse: String,
    
    // Surface Colors
    val surfaceBg: String,
    val surfaceCard: String,
    val surfaceHover: String,
    val surfaceActive: String,
    
    // Border Colors
    val border: String,
    val borderHover: String,
    val borderFocus: String,
    
    // Semantic Colors
    val success: String,
    val successLight: String,
    val warning: String,
    val warningLight: String,
    val error: String,
    val errorLight: String,
    val info: String,
    val infoLight: String
)

/**
 * 亮色模式
 */
object LightTheme {
    val colors = ColorTheme(
        primary = IndigoScale.c600,
        primaryHover = IndigoScale.c700,
        primaryActive = IndigoScale.c800,
        
        accent = CyanScale.c500,
        accentHover = CyanScale.c600,
        
        textPrimary = NeutralScale.c900,
        textSecondary = NeutralScale.c700,
        textTertiary = NeutralScale.c500,
        textInverse = NeutralScale.c50,
        
        surfaceBg = NeutralScale.c50,
        surfaceCard = "#ffffff",
        surfaceHover = NeutralScale.c100,
        surfaceActive = NeutralScale.c200,
        
        border = NeutralScale.c200,
        borderHover = NeutralScale.c300,
        borderFocus = IndigoScale.c600,
        
        success = GreenScale.c600,
        successLight = GreenScale.c100,
        warning = AmberScale.c500,
        warningLight = AmberScale.c100,
        error = RedScale.c600,
        errorLight = RedScale.c100,
        info = BlueScale.c500,
        infoLight = BlueScale.c100
    )
}

/**
 * 暗色模式
 */
object DarkTheme {
    val colors = ColorTheme(
        primary = IndigoScale.c300,
        primaryHover = IndigoScale.c400,
        primaryActive = IndigoScale.c500,
        
        accent = CyanScale.c400,
        accentHover = CyanScale.c500,
        
        textPrimary = NeutralScale.c100,
        textSecondary = NeutralScale.c300,
        textTertiary = NeutralScale.c500,
        textInverse = NeutralScale.c900,
        
        surfaceBg = NeutralScale.c900,
        surfaceCard = NeutralScale.c800,
        surfaceHover = NeutralScale.c700,
        surfaceActive = NeutralScale.c600,
        
        border = NeutralScale.c700,
        borderHover = NeutralScale.c600,
        borderFocus = IndigoScale.c300,
        
        success = GreenScale.c300,
        successLight = GreenScale.c900,
        warning = AmberScale.c300,
        warningLight = AmberScale.c900,
        error = RedScale.c300,
        errorLight = RedScale.c900,
        info = BlueScale.c300,
        infoLight = BlueScale.c900
    )
}

/**
 * 主题模式枚举
 */
enum class ThemeMode {
    LIGHT,
    DARK
}

/**
 * 获取主题
 */
fun getTheme(mode: ThemeMode = ThemeMode.DARK): ColorTheme {
    return when (mode) {
        ThemeMode.LIGHT -> LightTheme.colors
        ThemeMode.DARK -> DarkTheme.colors
    }
}

