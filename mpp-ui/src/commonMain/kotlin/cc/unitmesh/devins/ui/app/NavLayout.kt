package cc.unitmesh.devins.ui.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import cc.unitmesh.agent.AgentType
import cc.unitmesh.devins.ui.session.SessionViewModel
import kotlinx.coroutines.launch

/**
 * NavLayout - 跨平台导航布局系统
 *
 * 支持多平台导航模式：
 * 1. Android: Drawer + BottomNavigation
 * 2. iOS/macOS: TabBar (Material 3 风格)
 * 3. Desktop: NavigationRail
 *
 * 核心功能：
 * - 统一的导航状态管理
 * - 支持登录/登出
 * - 多屏幕切换
 * - 平台特定的 UI 适配
 *
 * 设计原则：
 * - Android：Material 3 设计，Drawer + BottomNav
 * - iOS/macOS：Material 3 TabBar + Settings Sheet
 * - Desktop：侧边 NavigationRail
 */

/**
 * 应用主屏幕定义
 */
enum class AppScreen {
    LOGIN,          // 登录
    CHAT,           // 本地对话（Local Chat）
    CODING,         // 编码 Agent
    CODE_REVIEW,    // 代码审查
    REMOTE,         // 云端服务器
    PROJECTS,       // 项目管理
    TASKS,          // 任务管理
    SESSIONS,       // 会话历史
    PROFILE,        // 个人中心
    HOME            // 兼容旧代码
}

/**
 * 导航项配置
 */
data class NavItem(
    val screen: AppScreen,
    val icon: ImageVector,
    val label: String,
    val description: String = "",
    val showInBottomNav: Boolean = true,    // Android BottomNavigation
    val showInTabBar: Boolean = true,        // iOS TabBar
    val showInDrawer: Boolean = true,        // Android Drawer
    val showInRail: Boolean = true           // Desktop NavigationRail
)

/**
 * 默认导航配置
 *
 * 主要功能（Bottom/Tab）：
 * - Chat: 本地 AI 对话
 * - Coding: 编码 Agent
 * - Review: 代码审查
 * - Remote: 云端服务器
 * - Profile: 个人中心
 *
 * 次要功能（Drawer only）：
 * - Projects: 项目管理
 * - Tasks: 任务管理
 * - Sessions: 会话历史
 */
val defaultNavItems = listOf(
    NavItem(
        screen = AppScreen.CHAT,
        icon = Icons.AutoMirrored.Filled.Chat,
        label = "对话",
        description = "本地 AI 对话",
        showInBottomNav = true,
        showInTabBar = true
    ),
    NavItem(
        screen = AppScreen.CODE_REVIEW,
        icon = Icons.Default.Reviews,
        label = "审查",
        description = "代码审查",
        showInBottomNav = true,
        showInTabBar = true
    ),
    NavItem(
        screen = AppScreen.REMOTE,
        icon = Icons.Default.Cloud,
        label = "云端",
        description = "云端智能体",
        showInBottomNav = true,
        showInTabBar = true
    ),
    NavItem(
        screen = AppScreen.PROFILE,
        icon = Icons.Default.Person,
        label = "我的",
        description = "个人中心",
        showInBottomNav = true,
        showInTabBar = true
    ),

    // 次要功能 - 仅在 Drawer 显示
    NavItem(
        screen = AppScreen.PROJECTS,
        icon = Icons.Default.Folder,
        label = "项目",
        description = "项目管理",
        showInBottomNav = false,
        showInTabBar = false,
        showInDrawer = true
    ),
    NavItem(
        screen = AppScreen.TASKS,
        icon = Icons.Default.Assignment,
        label = "任务",
        description = "任务管理",
        showInBottomNav = false,
        showInTabBar = false,
        showInDrawer = true
    ),
    NavItem(
        screen = AppScreen.SESSIONS,
        icon = Icons.Default.History,
        label = "会话",
        description = "会话历史",
        showInBottomNav = false,
        showInTabBar = false,
        showInDrawer = true
    )
)

/**
 * Android 风格的导航布局（Drawer + BottomNavigation）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MobileNavLayout(
    currentScreen: AppScreen,
    onScreenChange: (AppScreen) -> Unit,
    sessionViewModel: SessionViewModel,
    onShowSettings: () -> Unit = {},
    onShowTools: () -> Unit = {},
    onShowDebug: () -> Unit = {},
    hasDebugInfo: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val currentUser by sessionViewModel.currentUser.collectAsState()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                AndroidDrawerContent(
                    currentScreen = currentScreen,
                    currentUser = currentUser,
                    onScreenChange = { screen ->
                        onScreenChange(screen)
                        scope.launch { drawerState.close() }
                    },
                    onLogout = {
                        scope.launch {
                            sessionViewModel.logout()
                            drawerState.close()
                        }
                    },
                    onShowSettings = {
                        onShowSettings()
                        scope.launch { drawerState.close() }
                    },
                    onShowTools = {
                        onShowTools()
                        scope.launch { drawerState.close() }
                    },
                    onShowDebug = {
                        onShowDebug()
                        scope.launch { drawerState.close() }
                    },
                    hasDebugInfo = hasDebugInfo
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(getScreenTitle(currentScreen)) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "菜单")
                        }
                    },
                    actions = actions
                )
            },
            bottomBar = {
                NavigationBar {
                    defaultNavItems.filter { it.showInBottomNav }.forEach { navItem ->
                        NavigationBarItem(
                            icon = { Icon(navItem.icon, contentDescription = navItem.label) },
                            label = { Text(navItem.label) },
                            selected = currentScreen == navItem.screen,
                            onClick = { onScreenChange(navItem.screen) }
                        )
                    }
                }
            }
        ) { paddingValues ->
            content(paddingValues)
        }
    }
}

/**
 * Android Drawer 内容
 */
@Composable
private fun AndroidDrawerContent(
    currentScreen: AppScreen,
    currentUser: String?,
    onScreenChange: (AppScreen) -> Unit,
    onLogout: () -> Unit,
    onShowSettings: () -> Unit = {},
    onShowTools: () -> Unit = {},
    onShowDebug: () -> Unit = {},
    hasDebugInfo: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 用户信息区域
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = MaterialTheme.shapes.medium
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = "用户",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Column {
                    Text(
                        text = currentUser ?: "本地用户",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "AutoDev v0.2.0",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(8.dp))

        // 主导航项
        Text(
            text = "主要功能",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        defaultNavItems.filter { it.showInBottomNav }.forEach { navItem ->
            NavigationDrawerItem(
                icon = { Icon(navItem.icon, contentDescription = navItem.label) },
                label = { Text(navItem.label) },
                badge = if (navItem.description.isNotEmpty()) {
                    { Text(navItem.description, style = MaterialTheme.typography.labelSmall) }
                } else null,
                selected = currentScreen == navItem.screen,
                onClick = { onScreenChange(navItem.screen) },
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(8.dp))

        // 次要功能
        Text(
            text = "更多",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        defaultNavItems.filter { it.showInDrawer && !it.showInBottomNav }.forEach { navItem ->
            NavigationDrawerItem(
                icon = { Icon(navItem.icon, contentDescription = navItem.label) },
                label = { Text(navItem.label) },
                selected = currentScreen == navItem.screen,
                onClick = { onScreenChange(navItem.screen) },
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        HorizontalDivider()
        Spacer(modifier = Modifier.height(8.dp))

        // 设置和工具区域
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Settings, contentDescription = "模型设置") },
            label = { Text("模型设置") },
            selected = false,
            onClick = onShowSettings,
            modifier = Modifier.padding(vertical = 2.dp)
        )

        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Build, contentDescription = "工具配置") },
            label = { Text("工具配置") },
            selected = false,
            onClick = onShowTools,
            modifier = Modifier.padding(vertical = 2.dp)
        )

        if (hasDebugInfo) {
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.BugReport, contentDescription = "调试信息") },
                label = { Text("调试信息") },
                selected = false,
                onClick = onShowDebug,
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(8.dp))

        // 退出登录
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.ExitToApp, contentDescription = "退出登录") },
            label = { Text("退出登录") },
            selected = false,
            onClick = onLogout,
            colors = NavigationDrawerItemDefaults.colors(
                unselectedTextColor = MaterialTheme.colorScheme.error,
                unselectedIconColor = MaterialTheme.colorScheme.error
            )
        )
    }
}

/**
 * Apple (iOS/macOS) 导航布局
 *
 * 使用 Material 3 组件实现的 TabBar 风格导航
 * 包含底部 TabBar 和设置 Sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppleNavLayout(
    currentScreen: AppScreen,
    onScreenChange: (AppScreen) -> Unit,
    sessionViewModel: SessionViewModel,
    onShowSettings: () -> Unit = {},
    onShowTools: () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    val scope = rememberCoroutineScope()
    val currentUser by sessionViewModel.currentUser.collectAsState()

    // Apple 风格：无 Drawer，使用 Settings Sheet
    var showSettingsSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(getScreenTitle(currentScreen)) },
                actions = {
                    actions()
                    IconButton(onClick = { showSettingsSheet = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                }
            )
        },
        bottomBar = {
            // Apple 风格的 TabBar
            NavigationBar {
                defaultNavItems.filter { it.showInTabBar }.forEach { navItem ->
                    NavigationBarItem(
                        icon = { Icon(navItem.icon, contentDescription = navItem.label) },
                        label = { Text(navItem.label) },
                        selected = currentScreen == navItem.screen,
                        onClick = { onScreenChange(navItem.screen) }
                    )
                }
            }
        }
    ) { paddingValues ->
        content(paddingValues)
    }

    // Settings Sheet
    if (showSettingsSheet) {
        AppleSettingsSheet(
            currentUser = currentUser,
            onDismiss = { showSettingsSheet = false },
            onShowSettings = {
                onShowSettings()
                showSettingsSheet = false
            },
            onShowTools = {
                onShowTools()
                showSettingsSheet = false
            },
            onLogout = {
                scope.launch {
                    sessionViewModel.logout()
                }
                showSettingsSheet = false
            },
            onNavigate = { screen ->
                onScreenChange(screen)
                showSettingsSheet = false
            }
        )
    }
}

/**
 * Apple 设置面板（Bottom Sheet）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppleSettingsSheet(
    currentUser: String?,
    onDismiss: () -> Unit,
    onShowSettings: () -> Unit,
    onShowTools: () -> Unit,
    onLogout: () -> Unit,
    onNavigate: (AppScreen) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 用户信息
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = "用户",
                    modifier = Modifier.size(48.dp)
                )
                Column {
                    Text(
                        text = currentUser ?: "本地用户",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "AutoDev v0.2.0",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            HorizontalDivider()

            // 次要导航
            defaultNavItems.filter { !it.showInTabBar && it.showInDrawer }.forEach { navItem ->
                ListItem(
                    headlineContent = { Text(navItem.label) },
                    supportingContent = { Text(navItem.description) },
                    leadingContent = { Icon(navItem.icon, contentDescription = navItem.label) },
                    modifier = Modifier.clickable { onNavigate(navItem.screen) }
                )
            }

            HorizontalDivider()

            // 设置项
            ListItem(
                headlineContent = { Text("模型设置") },
                leadingContent = { Icon(Icons.Default.Settings, contentDescription = "模型设置") },
                modifier = Modifier.clickable { onShowSettings() }
            )

            ListItem(
                headlineContent = { Text("工具配置") },
                leadingContent = { Icon(Icons.Default.Build, contentDescription = "工具配置") },
                modifier = Modifier.clickable { onShowTools() }
            )

            HorizontalDivider()

            // 退出登录
            ListItem(
                headlineContent = {
                    Text(
                        "退出登录",
                        color = MaterialTheme.colorScheme.error
                    )
                },
                leadingContent = {
                    Icon(
                        Icons.Default.ExitToApp,
                        contentDescription = "退出登录",
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                modifier = Modifier.clickable { onLogout() }
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * Desktop 风格的导航布局（NavigationRail）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesktopNavLayout(
    currentScreen: AppScreen,
    onScreenChange: (AppScreen) -> Unit,
    sessionViewModel: SessionViewModel,
    onShowSettings: () -> Unit = {},
    onShowTools: () -> Unit = {},
    content: @Composable () -> Unit
) {
    Row(modifier = Modifier.fillMaxSize()) {
        // 左侧导航栏
        NavigationRail(
            header = {
                IconButton(onClick = onShowSettings) {
                    Icon(Icons.Default.Settings, contentDescription = "设置")
                }
            }
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            defaultNavItems.filter { it.showInRail }.forEach { navItem ->
                NavigationRailItem(
                    icon = { Icon(navItem.icon, contentDescription = navItem.label) },
                    label = { Text(navItem.label) },
                    selected = currentScreen == navItem.screen,
                    onClick = { onScreenChange(navItem.screen) }
                )
            }
        }

        // 右侧内容区域
        Box(modifier = Modifier.weight(1f)) {
            content()
        }
    }
}

/**
 * 获取屏幕标题
 */
fun getScreenTitle(screen: AppScreen): String {
    return when (screen) {
        AppScreen.LOGIN -> "登录"
        AppScreen.HOME -> "首页"
        AppScreen.CHAT -> "AI 对话"
        AppScreen.CODING -> "编码 Agent"
        AppScreen.CODE_REVIEW -> "代码审查"
        AppScreen.REMOTE -> "云端智能体"
        AppScreen.PROJECTS -> "项目管理"
        AppScreen.TASKS -> "任务管理"
        AppScreen.SESSIONS -> "会话历史"
        AppScreen.PROFILE -> "个人中心"
    }
}

/**
 * Agent 类型到屏幕的映射
 */
fun agentTypeToScreen(agentType: AgentType): AppScreen {
    return when (agentType) {
        AgentType.LOCAL_CHAT -> AppScreen.CHAT
        AgentType.CODING -> AppScreen.CODING
        AgentType.CODE_REVIEW -> AppScreen.CODE_REVIEW
        AgentType.REMOTE -> AppScreen.REMOTE
        else -> AppScreen.CHAT
    }
}

/**
 * 屏幕到 Agent 类型的映射
 */
fun screenToAgentType(screen: AppScreen): AgentType? {
    return when (screen) {
        AppScreen.CHAT -> AgentType.LOCAL_CHAT
        AppScreen.CODING -> AgentType.CODING
        AppScreen.CODE_REVIEW -> AgentType.CODE_REVIEW
        AppScreen.REMOTE -> AgentType.REMOTE
        else -> null
    }
}

