package cc.unitmesh.devins.ui.app

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import cc.unitmesh.devins.ui.compose.theme.AutoDevTheme
import cc.unitmesh.devins.ui.compose.theme.ThemeManager
import cc.unitmesh.devins.ui.project.ProjectListScreen
import cc.unitmesh.devins.ui.project.ProjectViewModel
import cc.unitmesh.devins.ui.remote.RemoteAgentClient
import cc.unitmesh.devins.ui.session.*
import cc.unitmesh.devins.ui.task.TaskExecutionScreen
import cc.unitmesh.devins.ui.task.TaskListScreen
import cc.unitmesh.devins.ui.task.TaskViewModel
import kotlinx.coroutines.launch

/**
 * SessionApp - 会话管理应用
 * 支持 Android 和 Desktop 平台
 *
 * 功能：
 * 1. 用户登录/注册
 * 2. 项目管理（创建、列表、Git 支持）
 * 3. 任务管理（基于项目创建任务）
 * 4. AI Agent 执行（集成 RemoteAgentClient）
 */
@Composable
fun SessionApp(
    serverUrl: String = "http://localhost:8080",
    useBottomNavigation: Boolean = false // Android 使用底部导航，Desktop 使用侧边导航
) {
    val currentTheme = ThemeManager.currentTheme

    AutoDevTheme(themeMode = currentTheme) {
        SessionAppContent(
            serverUrl = serverUrl,
            useBottomNavigation = useBottomNavigation
        )
    }
}

@Composable
private fun SessionAppContent(
    serverUrl: String,
    useBottomNavigation: Boolean
) {
    // 初始化客户端和 ViewModel
    val sessionClient = remember { SessionClient(serverUrl) }
    val remoteAgentClient = remember { RemoteAgentClient(serverUrl) }

    val projectClient = remember {
        cc.unitmesh.devins.ui.project.ProjectClient(
            serverUrl,
            sessionClient.httpClient
        )
    }

    val sessionViewModel = remember { SessionViewModel(sessionClient) }
    val projectViewModel = remember { ProjectViewModel(projectClient) }
    val taskViewModel = remember { TaskViewModel(sessionClient, remoteAgentClient) }

    val isAuthenticated by sessionViewModel.isAuthenticated.collectAsState()
    val currentProject by projectViewModel.currentProject.collectAsState()
    val currentTask by taskViewModel.currentTask.collectAsState()

    // 监听认证状态，同步 token
    LaunchedEffect(isAuthenticated, sessionClient.authToken) {
        if (isAuthenticated && sessionClient.authToken != null) {
            projectClient.setAuthToken(sessionClient.authToken!!)
        }
    }

    // 屏幕状态管理
    var currentScreen by remember { mutableStateOf(AppScreen.LOGIN) }

    // 监听认证状态
    LaunchedEffect(isAuthenticated) {
        if (isAuthenticated && currentScreen == AppScreen.LOGIN) {
            currentScreen = AppScreen.PROJECTS
        } else if (!isAuthenticated) {
            currentScreen = AppScreen.LOGIN
        }
    }

    // 监听项目选择
    LaunchedEffect(currentProject) {
        currentProject?.let {
            taskViewModel.setCurrentProject(it)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            sessionViewModel.onCleared()
            projectViewModel.onCleared()
            taskViewModel.onCleared()
        }
    }

    when {
        !isAuthenticated -> {
            LoginScreen(
                viewModel = sessionViewModel,
                onLoginSuccess = {
                    currentScreen = AppScreen.PROJECTS
                }
            )
        }
        currentTask != null -> {
            // 任务执行界面（全屏）
            TaskExecutionScreen(
                viewModel = taskViewModel,
                onBack = {
                    currentScreen = AppScreen.TASKS
                }
            )
        }
        useBottomNavigation -> {
            // Android 底部导航
            AndroidNavigationLayout(
                currentScreen = currentScreen,
                onScreenChange = { currentScreen = it },
                sessionViewModel = sessionViewModel,
                projectViewModel = projectViewModel,
                taskViewModel = taskViewModel
            )
        }
        else -> {
            // Desktop 侧边导航
            DesktopNavigationLayout(
                currentScreen = currentScreen,
                onScreenChange = { currentScreen = it },
                sessionViewModel = sessionViewModel,
                projectViewModel = projectViewModel,
                taskViewModel = taskViewModel
            )
        }
    }
}

/**
 * Android 底部导航布局
 */
@Composable
private fun AndroidNavigationLayout(
    currentScreen: AppScreen,
    onScreenChange: (AppScreen) -> Unit,
    sessionViewModel: SessionViewModel,
    projectViewModel: ProjectViewModel,
    taskViewModel: TaskViewModel
) {
    val scope = rememberCoroutineScope()

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Folder, contentDescription = "项目") },
                    label = { Text("项目") },
                    selected = currentScreen == AppScreen.PROJECTS,
                    onClick = { onScreenChange(AppScreen.PROJECTS) }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Assignment, contentDescription = "任务") },
                    label = { Text("任务") },
                    selected = currentScreen == AppScreen.TASKS,
                    onClick = { onScreenChange(AppScreen.TASKS) }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.History, contentDescription = "会话") },
                    label = { Text("会话") },
                    selected = currentScreen == AppScreen.SESSIONS,
                    onClick = { onScreenChange(AppScreen.SESSIONS) }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = "我的") },
                    label = { Text("我的") },
                    selected = currentScreen == AppScreen.PROFILE,
                    onClick = { onScreenChange(AppScreen.PROFILE) }
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (currentScreen) {
                AppScreen.PROJECTS -> {
                    ProjectListScreen(
                        viewModel = projectViewModel,
                        onProjectClick = { project ->
                            projectViewModel.selectProject(project)
                            onScreenChange(AppScreen.TASKS)
                        }
                    )
                }
                AppScreen.TASKS -> {
                    TaskListScreen(
                        viewModel = taskViewModel,
                        onTaskClick = { task ->
                            // 任务点击会自动切换到 TaskExecutionScreen
                        }
                    )
                }
                AppScreen.SESSIONS -> {
                    SessionListScreen(
                        viewModel = sessionViewModel,
                        onSessionClick = { session ->
                            scope.launch {
                                sessionViewModel.joinSession(session.id)
                            }
                        },
                        onCreateSession = {
                            // 创建会话
                        },
                        onLogout = {
                            scope.launch {
                                sessionViewModel.logout()
                            }
                        }
                    )
                }
                AppScreen.PROFILE -> {
                    ProfileScreen(
                        viewModel = sessionViewModel,
                        onLogout = {
                            scope.launch {
                                sessionViewModel.logout()
                            }
                        }
                    )
                }
                else -> {}
            }
        }
    }
}

/**
 * Desktop 侧边导航布局
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DesktopNavigationLayout(
    currentScreen: AppScreen,
    onScreenChange: (AppScreen) -> Unit,
    sessionViewModel: SessionViewModel,
    projectViewModel: ProjectViewModel,
    taskViewModel: TaskViewModel
) {
    val scope = rememberCoroutineScope()

    Row(modifier = Modifier.fillMaxSize()) {
        // 左侧导航栏
        NavigationRail {
            NavigationRailItem(
                icon = { Icon(Icons.Default.Folder, contentDescription = "项目") },
                label = { Text("项目") },
                selected = currentScreen == AppScreen.PROJECTS,
                onClick = { onScreenChange(AppScreen.PROJECTS) }
            )
            NavigationRailItem(
                icon = { Icon(Icons.Default.Assignment, contentDescription = "任务") },
                label = { Text("任务") },
                selected = currentScreen == AppScreen.TASKS,
                onClick = { onScreenChange(AppScreen.TASKS) }
            )
            NavigationRailItem(
                icon = { Icon(Icons.Default.History, contentDescription = "会话") },
                label = { Text("会话") },
                selected = currentScreen == AppScreen.SESSIONS,
                onClick = { onScreenChange(AppScreen.SESSIONS) }
            )
            NavigationRailItem(
                icon = { Icon(Icons.Default.Person, contentDescription = "我的") },
                label = { Text("我的") },
                selected = currentScreen == AppScreen.PROFILE,
                onClick = { onScreenChange(AppScreen.PROFILE) }
            )
        }

        // 右侧内容区域
        Box(modifier = Modifier.weight(1f)) {
            when (currentScreen) {
                AppScreen.PROJECTS -> {
                    ProjectListScreen(
                        viewModel = projectViewModel,
                        onProjectClick = { project ->
                            projectViewModel.selectProject(project)
                            onScreenChange(AppScreen.TASKS)
                        }
                    )
                }
                AppScreen.TASKS -> {
                    TaskListScreen(
                        viewModel = taskViewModel,
                        onTaskClick = { task ->
                            // 任务点击会自动切换到 TaskExecutionScreen
                        }
                    )
                }
                AppScreen.SESSIONS -> {
                    SessionListScreen(
                        viewModel = sessionViewModel,
                        onSessionClick = { session ->
                            scope.launch {
                                sessionViewModel.joinSession(session.id)
                            }
                        },
                        onCreateSession = {
                            // 创建会话
                        },
                        onLogout = {
                            scope.launch {
                                sessionViewModel.logout()
                            }
                        }
                    )
                }
                AppScreen.PROFILE -> {
                    ProfileScreen(
                        viewModel = sessionViewModel,
                        onLogout = {
                            scope.launch {
                                sessionViewModel.logout()
                            }
                        }
                    )
                }
                else -> {}
            }
        }
    }
}

enum class AppScreen {
    LOGIN,
    PROJECTS,
    TASKS,
    SESSIONS,
    PROFILE
}

