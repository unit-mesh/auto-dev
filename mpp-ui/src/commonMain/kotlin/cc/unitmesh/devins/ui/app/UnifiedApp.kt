package cc.unitmesh.devins.ui.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cc.unitmesh.agent.Platform
import cc.unitmesh.devins.ui.project.CreateProjectDialog
import cc.unitmesh.devins.ui.project.Project
import cc.unitmesh.devins.ui.project.ProjectClient
import cc.unitmesh.devins.ui.project.ProjectViewModel
import cc.unitmesh.devins.ui.remote.RemoteAgentClient
import cc.unitmesh.devins.ui.session.*
import cc.unitmesh.devins.ui.task.CreateTaskDialog
import cc.unitmesh.devins.ui.task.TaskExecutionScreen
import cc.unitmesh.devins.ui.task.TaskViewModel
import kotlinx.coroutines.launch

@Composable
internal fun UnifiedAppContent(
    serverUrl: String,
    onOpenLocalChat: (() -> Unit)? = null
) {
    // 初始化客户端和 ViewModel
    val sessionClient = remember { SessionClient(serverUrl) }
    val remoteAgentClient = remember { RemoteAgentClient(serverUrl) }
    val projectClient = remember {
        ProjectClient(serverUrl, sessionClient.httpClient)
    }

    val sessionViewModel = remember { SessionViewModel(sessionClient) }
    val projectViewModel = remember { ProjectViewModel(projectClient) }
    val taskViewModel = remember { TaskViewModel(sessionClient, remoteAgentClient) }

    val isAuthenticated by sessionViewModel.isAuthenticated.collectAsState()
    val currentTask by taskViewModel.currentTask.collectAsState()

    LaunchedEffect(isAuthenticated, sessionClient.authToken) {
        if (isAuthenticated && sessionClient.authToken != null) {
            projectClient.setAuthToken(sessionClient.authToken!!)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            sessionViewModel.onCleared()
            projectViewModel.onCleared()
            taskViewModel.onCleared()
        }
    }

    // 屏幕状态管理（用于 Android NavLayout）
    var currentScreen by remember { mutableStateOf(AppScreen.PROJECTS) }
    var skipLogin by remember { mutableStateOf(false) }

    // 监听认证状态
    LaunchedEffect(isAuthenticated) {
        if (isAuthenticated && currentScreen == AppScreen.LOGIN) {
            currentScreen = AppScreen.PROJECTS
        } else if (!isAuthenticated && !skipLogin) {
            currentScreen = AppScreen.LOGIN
        }
    }

    when {
        !isAuthenticated && !skipLogin -> {
            LoginScreen(
                viewModel = sessionViewModel,
                onLoginSuccess = {
                    currentScreen = AppScreen.PROJECTS
                },
                onSkipLogin = if (Platform.isAndroid) {
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
        Platform.isAndroid -> {
            // Android: 使用 NavLayout（Drawer + BottomNavigation）
            AndroidUnifiedLayout(
                currentScreen = currentScreen,
                onScreenChange = { currentScreen = it },
                sessionViewModel = sessionViewModel,
                projectViewModel = projectViewModel,
                taskViewModel = taskViewModel
            )
        }
        else -> {
            // Desktop: 使用侧边栏布局
            DesktopMainLayout(
                sessionViewModel = sessionViewModel,
                projectViewModel = projectViewModel,
                taskViewModel = taskViewModel,
                onOpenLocalChat = onOpenLocalChat
            )
        }
    }
}

/**
 * Android 统一布局 - 使用 NavLayout
 */
@Composable
private fun AndroidUnifiedLayout(
    currentScreen: AppScreen,
    onScreenChange: (AppScreen) -> Unit,
    sessionViewModel: SessionViewModel,
    projectViewModel: ProjectViewModel,
    taskViewModel: TaskViewModel
) {
    val scope = rememberCoroutineScope()
    val sessions by sessionViewModel.sessions.collectAsState()
    val currentProject by projectViewModel.currentProject.collectAsState()

    AndroidNavLayout(
        currentScreen = currentScreen,
        onScreenChange = onScreenChange,
        sessionViewModel = sessionViewModel
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (currentScreen) {
                AppScreen.PROJECTS -> {
                    cc.unitmesh.devins.ui.project.ProjectListScreen(
                        viewModel = projectViewModel,
                        onProjectClick = { project ->
                            projectViewModel.selectProject(project)
                            onScreenChange(AppScreen.TASKS)
                        }
                    )
                }
                AppScreen.TASKS -> {
                    cc.unitmesh.devins.ui.task.TaskListScreen(
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
 * Desktop 主布局 - 使用侧边栏
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DesktopMainLayout(
    sessionViewModel: SessionViewModel,
    projectViewModel: ProjectViewModel,
    taskViewModel: TaskViewModel,
    onOpenLocalChat: (() -> Unit)?
) {
    val scope = rememberCoroutineScope()

    // 当前选中的内容类型
    var selectedView by remember { mutableStateOf(ViewType.SESSIONS) }
    var selectedSessionId by remember { mutableStateOf<String?>(null) }
    var selectedProjectId by remember { mutableStateOf<String?>(null) }

    // Dialog 状态
    var showCreateSessionDialog by remember { mutableStateOf(false) }
    var showCreateProjectDialog by remember { mutableStateOf(false) }
    var showCreateTaskDialog by remember { mutableStateOf(false) }

    val sessions by sessionViewModel.sessions.collectAsState()
    val projects by projectViewModel.projects.collectAsState()
    val currentProject by projectViewModel.currentProject.collectAsState()

    // 加载数据
    LaunchedEffect(Unit) {
        sessionViewModel.loadSessions()
        projectViewModel.loadProjects()
    }

    Row(modifier = Modifier.fillMaxSize()) {
        // 左侧边栏
        Surface(
            modifier = Modifier.width(280.dp).fillMaxHeight(),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 1.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 顶部：新建按钮区域
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 本地 Chat 按钮（仅 Desktop）
                    onOpenLocalChat?.let { openChat ->
                        Button(
                            onClick = openChat,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Default.Chat, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("本地 Chat")
                        }
                    }

                    // 新建按钮
                    OutlinedButton(
                        onClick = { showCreateSessionDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("新建 Session")
                    }

                    OutlinedButton(
                        onClick = { showCreateProjectDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("新建 Project")
                    }

                    if (currentProject != null) {
                        OutlinedButton(
                            onClick = { showCreateTaskDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("新建 Task")
                        }
                    }
                }

                HorizontalDivider()

                // 中间：列表区域（可滚动）
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Sessions 标题
                    item {
                        SidebarSectionHeader(
                            title = "Sessions",
                            isExpanded = selectedView == ViewType.SESSIONS,
                            onClick = { selectedView = ViewType.SESSIONS }
                        )
                    }

                    if (selectedView == ViewType.SESSIONS) {
                        items(sessions) { session ->
                            SidebarItem(
                                icon = Icons.Default.History,
                                title = session.task,
                                subtitle = "ID: ${session.id.take(8)}",
                                isSelected = selectedSessionId == session.id,
                                onClick = {
                                    selectedSessionId = session.id
                                    scope.launch {
                                        sessionViewModel.joinSession(session.id)
                                    }
                                }
                            )
                        }
                    }

                    // Projects 标题
                    item {
                        SidebarSectionHeader(
                            title = "Projects",
                            isExpanded = selectedView == ViewType.PROJECTS,
                            onClick = { selectedView = ViewType.PROJECTS }
                        )
                    }

                    if (selectedView == ViewType.PROJECTS) {
                        items(projects) { project ->
                            SidebarItem(
                                icon = Icons.Default.Folder,
                                title = project.name,
                                subtitle = project.description ?: "",
                                isSelected = selectedProjectId == project.id,
                                onClick = {
                                    selectedProjectId = project.id
                                    projectViewModel.selectProject(project)
                                }
                            )
                        }
                    }
                }

                HorizontalDivider()

                // 底部：Settings 和 Profile
                Column(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    SidebarItem(
                        icon = Icons.Default.Settings,
                        title = "Settings",
                        onClick = { /* TODO: 打开设置 */ }
                    )

                    SidebarItem(
                        icon = Icons.Default.Person,
                        title = sessionViewModel.currentUser.collectAsState().value ?: "Profile",
                        onClick = { /* TODO: 打开个人信息 */ }
                    )

                    SidebarItem(
                        icon = Icons.Default.ExitToApp,
                        title = "退出登录",
                        onClick = {
                            scope.launch {
                                sessionViewModel.logout()
                            }
                        }
                    )
                }
            }
        }

        // 右侧内容区域
        Box(
            modifier = Modifier.weight(1f).fillMaxHeight()
                .background(MaterialTheme.colorScheme.background)
        ) {
            when {
                selectedSessionId != null -> {
                    SessionDetailScreen(
                        viewModel = sessionViewModel,
                        onBack = { selectedSessionId = null }
                    )
                }
                selectedProjectId != null -> {
                    currentProject?.let { project ->
                        // 显示项目的任务列表
                        TaskListContent(
                            viewModel = taskViewModel,
                            project = project
                        )
                    }
                }
                else -> {
                    // 欢迎界面
                    WelcomeScreen()
                }
            }
        }
    }

    // Dialogs
    if (showCreateSessionDialog) {
        CreateSessionDialog(
            viewModel = sessionViewModel,
            onDismiss = { showCreateSessionDialog = false }
        )
    }

    if (showCreateProjectDialog) {
        CreateProjectDialog(
            viewModel = projectViewModel,
            onDismiss = { showCreateProjectDialog = false }
        )
    }

    if (showCreateTaskDialog) {
        CreateTaskDialog(
            viewModel = taskViewModel,
            onDismiss = { showCreateTaskDialog = false }
        )
    }
}

@Composable
private fun SidebarSectionHeader(
    title: String,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isExpanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SidebarItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String = "",
    isSelected: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                if (subtitle.isNotEmpty()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun TaskListContent(
    viewModel: TaskViewModel,
    project: Project
) {
    val tasks by viewModel.tasks.collectAsState()

    LaunchedEffect(project) {
        viewModel.setCurrentProject(project)
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        Text(
            text = "项目: ${project.name}",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = project.description ?: "",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (tasks.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "暂无任务，点击左侧「新建 Task」创建",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(tasks) { task ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            viewModel.executeTask(task)
                        }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = task.task,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "ID: ${task.id}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomeScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "欢迎使用 AutoDev",
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = "从左侧选择 Session 或 Project 开始",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

enum class ViewType {
    SESSIONS,
    PROJECTS
}

