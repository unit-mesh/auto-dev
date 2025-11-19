package cc.unitmesh.devins.ui.app

import androidx.compose.foundation.layout.*
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
    var skipLogin by remember { mutableStateOf(false) }

    // 监听认证状态
    LaunchedEffect(isAuthenticated) {
        if (isAuthenticated && currentScreen == AppScreen.LOGIN) {
            currentScreen = AppScreen.PROJECTS
        } else if (!isAuthenticated && !skipLogin) {
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
        !isAuthenticated && !skipLogin -> {
            LoginScreen(
                viewModel = sessionViewModel,
                onLoginSuccess = {
                    currentScreen = AppScreen.PROJECTS
                },
                onSkipLogin = if (useBottomNavigation) {
                    // Android 平台允许跳过登录
                    {
                        skipLogin = true
                        currentScreen = AppScreen.PROJECTS
                    }
                } else null
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
 * 使用新的 NavLayout 组件，支持 Drawer + BottomNavigation
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

    MobileNavLayout(
        currentScreen = currentScreen,
        onScreenChange = onScreenChange,
        sessionViewModel = sessionViewModel
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
 * 使用新的 NavLayout 组件
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

    DesktopNavLayout(
        currentScreen = currentScreen,
        onScreenChange = onScreenChange,
        sessionViewModel = sessionViewModel
    ) {
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

/**
 * SessionAppContext - Session 应用上下文
 *
 * 用于管理 Session 应用的全局状态和配置
 * 参考 SessionAppContext 的设计，提供统一的上下文管理
 */
data class SessionAppContext(
    val serverUrl: String,
    val useBottomNavigation: Boolean,
    val sessionViewModel: SessionViewModel,
    val projectViewModel: ProjectViewModel,
    val taskViewModel: TaskViewModel,
    val currentScreen: AppScreen = AppScreen.LOGIN,
    val skipLogin: Boolean = false,
    val isAuthenticated: Boolean = false
) {
    companion object {
        /**
         * 创建默认的 SessionAppContext
         */
        fun create(
            serverUrl: String,
            useBottomNavigation: Boolean,
            sessionClient: SessionClient,
            projectClient: cc.unitmesh.devins.ui.project.ProjectClient,
            remoteAgentClient: cc.unitmesh.devins.ui.remote.RemoteAgentClient
        ): SessionAppContext {
            return SessionAppContext(
                serverUrl = serverUrl,
                useBottomNavigation = useBottomNavigation,
                sessionViewModel = SessionViewModel(sessionClient),
                projectViewModel = ProjectViewModel(projectClient),
                taskViewModel = TaskViewModel(sessionClient, remoteAgentClient)
            )
        }
    }
}

