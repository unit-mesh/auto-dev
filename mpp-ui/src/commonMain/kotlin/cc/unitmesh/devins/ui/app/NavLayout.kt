package cc.unitmesh.devins.ui.app

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cc.unitmesh.devins.ui.session.SessionViewModel
import kotlinx.coroutines.launch

/**
 * NavLayout - 统一的导航布局组件
 *
 * 支持多种导航模式：
 * 1. Android: Drawer + BottomNavigation
 * 2. Desktop: NavigationRail
 *
 * 功能：
 * - 统一的导航状态管理
 * - 支持登录/登出
 * - 支持多屏幕切换
 * - 提供一致的用户体验
 */

/**
 * 导航项配置
 */
data class NavItem(
    val screen: AppScreen,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val label: String,
    val showInBottomNav: Boolean = true,
    val showInDrawer: Boolean = true
)

/**
 * 默认导航项（支持 Home + Chat）
 */
val defaultNavItems = listOf(
    NavItem(
        screen = AppScreen.HOME,
        icon = Icons.Default.Home,
        label = "首页",
        showInBottomNav = true,
        showInDrawer = true
    ),
    NavItem(
        screen = AppScreen.CHAT,
        icon = Icons.Default.Chat,
        label = "对话",
        showInBottomNav = true,
        showInDrawer = true
    ),
    NavItem(
        screen = AppScreen.PROJECTS,
        icon = Icons.Default.Folder,
        label = "项目",
        showInBottomNav = false, // Android BottomNav 只显示 4 个
        showInDrawer = true
    ),
    NavItem(
        screen = AppScreen.TASKS,
        icon = Icons.Default.Assignment,
        label = "任务",
        showInBottomNav = true,
        showInDrawer = true
    ),
    NavItem(
        screen = AppScreen.PROFILE,
        icon = Icons.Default.Person,
        label = "我的",
        showInBottomNav = true,
        showInDrawer = true
    )
)

/**
 * NavLayout 配置
 */
data class NavLayoutConfig(
    val useDrawer: Boolean = false,
    val useBottomNav: Boolean = false,
    val showTopBar: Boolean = true,
    val navItems: List<NavItem> = defaultNavItems
)

/**
 * Android 风格的导航布局（Drawer + BottomNavigation）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AndroidNavLayout(
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
                DrawerContent(
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
 * Drawer 内容（增强版，支持设置和工具）
 */
@Composable
private fun DrawerContent(
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
                        text = "AutoDev",
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
        defaultNavItems.filter { it.showInDrawer }.forEach { navItem ->
            NavigationDrawerItem(
                icon = { Icon(navItem.icon, contentDescription = navItem.label) },
                label = { Text(navItem.label) },
                selected = currentScreen == navItem.screen,
                onClick = { onScreenChange(navItem.screen) },
                modifier = Modifier.padding(vertical = 4.dp)
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

        Spacer(modifier = Modifier.height(16.dp))

        // 版本信息
        Text(
            text = "AutoDev v0.1.5",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 获取屏幕标题
 */
private fun getScreenTitle(screen: AppScreen): String {
    return when (screen) {
        AppScreen.LOGIN -> "登录"
        AppScreen.HOME -> "首页"
        AppScreen.CHAT -> "AI 对话"
        AppScreen.PROJECTS -> "项目"
        AppScreen.TASKS -> "任务"
        AppScreen.SESSIONS -> "会话"
        AppScreen.PROFILE -> "我的"
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
    content: @Composable () -> Unit
) {
    Row(modifier = Modifier.fillMaxSize()) {
        // 左侧导航栏
        NavigationRail {
            defaultNavItems.forEach { navItem ->
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

