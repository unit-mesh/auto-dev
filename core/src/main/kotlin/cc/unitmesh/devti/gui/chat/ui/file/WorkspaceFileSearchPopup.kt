package cc.unitmesh.devti.gui.chat.ui.file

import cc.unitmesh.devti.util.canBeAdded
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.impl.EditorHistoryManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.JBPopupListener
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.ui.JBColor
import com.intellij.ui.SearchTextField
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.Alarm
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import org.jetbrains.annotations.NotNull
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.event.*
import javax.swing.*

class WorkspaceFileSearchPopup(
    private val project: Project,
    private val onFilesSelected: (List<VirtualFile>) -> Unit
) {
    companion object {
        private val LOG = Logger.getInstance(WorkspaceFileSearchPopup::class.java)
        private const val MAX_RECENT_FILES = 30
        private const val SEARCH_DELAY_MS = 300
        private const val BATCH_SIZE = 50
    }

    private var popup: JBPopup? = null
    private val fileListModel = DefaultListModel<FilePresentation>()
    private val fileList = JBList(fileListModel).apply {
        cellRenderer = FileListCellRenderer()
        selectionMode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
    }

    private val searchField = SearchTextField()
    private val contentPanel = JPanel(BorderLayout())
    private val loadingLabel = JBLabel("Loading files...", SwingConstants.CENTER).apply {
        isVisible = false
    }

    private val minPopupSize = Dimension(480, 320)
    private val searchAlarm = Alarm(Alarm.ThreadToUse.POOLED_THREAD)

    // 存储已加载的文件
    private val recentFiles = mutableListOf<FilePresentation>()
    private val projectFiles = mutableListOf<FilePresentation>()

    // 标志是否正在加载项目文件
    private var isLoadingFiles = false
    private var hasLoadedAllFiles = false
    private var currentSearchQuery = ""

    init {
        setupUI()
        loadRecentFiles()
    }

    private fun setupUI() {
        // Configure search field
        searchField.textEditor.addKeyListener(object : KeyAdapter() {
            override fun keyReleased(e: KeyEvent) {
                if (e.keyCode == KeyEvent.VK_DOWN && fileListModel.size > 0) {
                    fileList.requestFocus()
                    fileList.selectedIndex = 0
                    return
                }

                // 使用延迟搜索，避免每次按键都触发搜索
                val query = searchField.text.trim()
                scheduleSearch(query)
            }
        })

        // Configure file list
        fileList.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                when (e.keyCode) {
                    KeyEvent.VK_ENTER -> {
                        selectFiles()
                        e.consume()
                    }

                    KeyEvent.VK_ESCAPE -> {
                        popup?.cancel()
                        e.consume()
                    }

                    KeyEvent.VK_UP -> {
                        if (fileList.selectedIndex == 0) {
                            searchField.requestFocus()
                            e.consume()
                        }
                    }
                }
            }
        })

        fileList.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2) {
                    selectFiles()
                }
            }
        })

        // 添加滚动监听器来实现滚动加载更多文件
        val scrollPane = JBScrollPane(fileList)
        scrollPane.verticalScrollBar.addAdjustmentListener { e ->
            val scrollBar = e.adjustable
            // 当滚动到底部且还有更多文件可加载时，加载更多文件
            if (!isLoadingFiles && !hasLoadedAllFiles &&
                scrollBar.value + scrollBar.visibleAmount >= scrollBar.maximum - 10
            ) {
                loadMoreProjectFiles()
            }
        }

        // Setup layout with proper borders and spacing
        val statusPanel = JPanel(BorderLayout())
        statusPanel.add(loadingLabel, BorderLayout.CENTER)

        contentPanel.border = JBUI.Borders.empty()
        contentPanel.add(searchField, BorderLayout.NORTH)
        contentPanel.add(scrollPane, BorderLayout.CENTER)
        contentPanel.add(statusPanel, BorderLayout.SOUTH)
        contentPanel.preferredSize = minPopupSize
    }

    private fun scheduleSearch(query: String) {
        searchAlarm.cancelAllRequests()
        currentSearchQuery = query

        searchAlarm.addRequest({
            ApplicationManager.getApplication().invokeLater({
                if (query == currentSearchQuery) {
                    performSearch(query)
                }
            }, ModalityState.any())
        }, SEARCH_DELAY_MS)
    }

    private fun performSearch(query: String) {
        fileListModel.clear()

        if (query.isBlank()) {
            // 空查询，显示最近文件和已加载的项目文件
            showRecentAndLoadedFiles()
        } else if (query.length >= 2) {
            // 有查询词，使用索引搜索文件
            searchFilesWithQuery(query)
        } else {
            // 查询词太短，只显示最近文件
            showRecentFiles()
        }
    }

    private fun showRecentAndLoadedFiles() {
        // 添加最近文件
        val filesToShow = ArrayList<FilePresentation>()
        filesToShow.addAll(recentFiles)

        // 添加已加载的项目文件（不包括已显示的最近文件）
        val recentPaths = recentFiles.map { it.path }.toSet()
        filesToShow.addAll(projectFiles.filter { it.path !in recentPaths }.take(BATCH_SIZE))

        // 排序并显示
        filesToShow.sortWith(compareBy<FilePresentation> { !it.isRecentFile }.thenBy { it.name })
        filesToShow.forEach { fileListModel.addElement(it) }

        // 如果还没开始加载项目文件，则开始加载
        if (projectFiles.isEmpty() && !isLoadingFiles && !hasLoadedAllFiles) {
            startLoadingProjectFiles()
        }

        selectFirstItemIfAvailable()
    }

    private fun showRecentFiles() {
        recentFiles.forEach { fileListModel.addElement(it) }
        selectFirstItemIfAvailable()
    }

    private fun searchFilesWithQuery(query: String) {
        // 先搜索内存中已加载的文件
        val matchingLoaded = ArrayList<FilePresentation>()

        // 搜索最近文件
        matchingLoaded.addAll(recentFiles.filter {
            it.name.contains(query, ignoreCase = true) ||
                    it.path.contains(query, ignoreCase = true)
        })

        // 搜索已加载的项目文件
        val recentPaths = recentFiles.map { it.path }.toSet()
        matchingLoaded.addAll(projectFiles.filter {
            it.path !in recentPaths &&
                    (it.name.contains(query, ignoreCase = true) || it.path.contains(query, ignoreCase = true))
        })

        // 使用 IDEA 的索引API搜索额外的文件
        val scope = GlobalSearchScope.projectScope(project)
        val additionalFiles = mutableListOf<FilePresentation>()

        ApplicationManager.getApplication().runReadAction {
            // 按文件名搜索
            FilenameIndex.processFilesByName(query, false, scope) { file ->
                if (file.canBeAdded(project) &&
                    !matchingLoaded.any { it.path == file.path }
                ) {
                    additionalFiles.add(FilePresentation.from(project, file))
                }
                true
            }
        }

        // 合并结果并排序
        val allResults = ArrayList<FilePresentation>()
        allResults.addAll(matchingLoaded)
        allResults.addAll(additionalFiles)

        allResults.sortWith(compareBy<FilePresentation> { !it.isRecentFile }.thenBy { it.name })
        allResults.take(BATCH_SIZE).forEach { fileListModel.addElement(it) }

        selectFirstItemIfAvailable()
    }

    private fun loadRecentFiles() {
        recentFiles.clear()

        // 加载最近打开的文件
        val fileList = EditorHistoryManager.getInstance(project).fileList
        fileList.take(MAX_RECENT_FILES).forEach { file ->
            if (file.canBeAdded(project)) {
                val presentation = FilePresentation.from(project, file)
                presentation.isRecentFile = true
                recentFiles.add(presentation)
            }
        }

        // 初始显示最近文件
        showRecentFiles()
    }

    private fun startLoadingProjectFiles() {
        if (isLoadingFiles || hasLoadedAllFiles) return

        isLoadingFiles = true
        loadingLabel.isVisible = true

        // 在后台加载项目文件
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Loading Files", false) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    loadMoreProjectFilesInternal(indicator)
                } finally {
                    ApplicationManager.getApplication().invokeLater({
                        isLoadingFiles = false
                        loadingLabel.isVisible = false

                        // 如果有搜索查询，更新搜索结果
                        if (currentSearchQuery.isNotBlank()) {
                            performSearch(currentSearchQuery)
                        } else if (fileListModel.size == 0 || fileListModel.size == recentFiles.size) {
                            // 如果当前只显示了最近文件，添加新加载的项目文件
                            showRecentAndLoadedFiles()
                        }
                    }, ModalityState.any())
                }
            }
        })
    }

    private fun loadMoreProjectFiles() {
        if (isLoadingFiles || hasLoadedAllFiles) return

        isLoadingFiles = true
        loadingLabel.isVisible = true

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Loading More Files", false) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    loadMoreProjectFilesInternal(indicator)
                } finally {
                    ApplicationManager.getApplication().invokeLater({
                        isLoadingFiles = false
                        loadingLabel.isVisible = false

                        // 如果当前有搜索查询，更新搜索结果
                        if (currentSearchQuery.isNotBlank()) {
                            performSearch(currentSearchQuery)
                        } else {
                            // 添加新加载的项目文件到当前列表
                            val currentSize = fileListModel.size
                            val recentPaths = recentFiles.map { it.path }.toSet()
                            val currentPaths = (0 until currentSize).mapNotNull {
                                fileListModel.getElementAt(it)?.path
                            }.toSet()

                            projectFiles
                                .filter { it.path !in recentPaths && it.path !in currentPaths }
                                .take(BATCH_SIZE)
                                .forEach { fileListModel.addElement(it) }
                        }
                    }, ModalityState.any())
                }
            }
        })
    }

    private fun loadMoreProjectFilesInternal(indicator: ProgressIndicator) {
        indicator.isIndeterminate = false

        val currentSize = projectFiles.size
        val loadedPaths = (recentFiles + projectFiles).map { it.path }.toSet()
        val newFiles = mutableListOf<FilePresentation>()
        var count = 0

        // 使用项目文件索引迭代文件
        ProjectFileIndex.getInstance(project).iterateContent { file ->
            if (indicator.isCanceled) return@iterateContent false

            count++
            if (count % 100 == 0) {
                indicator.fraction = count.toDouble() / 10000.0
                indicator.text = "Scanned $count files..."
            }

            // 添加符合条件且尚未加载的文件
            if (file.canBeAdded(project) &&
                !ProjectFileIndex.getInstance(project).isUnderIgnored(file) &&
                ProjectFileIndex.getInstance(project).isInContent(file) &&
                file.path !in loadedPaths
            ) {
                newFiles.add(FilePresentation.from(project, file))

                // 每处理BATCH_SIZE个文件检查一次是否应该停止
                if (newFiles.size >= BATCH_SIZE) {
                    return@iterateContent false
                }
            }

            // 如果已经处理了超过10000个文件，也停止扫描
            if (count > 10000) {
                return@iterateContent false
            }

            true
        }

        // 更新加载状态
        synchronized(projectFiles) {
            projectFiles.addAll(newFiles)
            hasLoadedAllFiles = newFiles.size < BATCH_SIZE || count > 10000
        }

        LOG.info("Loaded ${newFiles.size} more files. Total loaded: ${projectFiles.size}")
    }

    private fun selectFirstItemIfAvailable() {
        if (fileListModel.size > 0) {
            fileList.selectedIndex = 0
        }
    }

    private fun selectFiles() {
        val selectedFiles = fileList.selectedValuesList.map { it.virtualFile }
        if (selectedFiles.isNotEmpty()) {
            onFilesSelected(selectedFiles)
            popup?.cancel()
        }
    }

    fun show(component: JComponent) {
        popup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(contentPanel, searchField.textEditor)
            .setTitle("Add Files to Workspace")
            .setMovable(true)
            .setResizable(true)
            .setRequestFocus(true)
            .setFocusable(true)
            .setCancelOnClickOutside(true)
            .setCancelOnOtherWindowOpen(true)
            .setMinSize(minPopupSize)
            .createPopup()

        popup?.addListener(object : JBPopupListener {
            override fun onClosed(event: LightweightWindowEvent) {
                // 取消所有后台任务
                searchAlarm.cancelAllRequests()

                // 清理资源
                recentFiles.clear()
                projectFiles.clear()
                fileListModel.clear()

                isLoadingFiles = false
                hasLoadedAllFiles = false
            }
        })

        // Show popup in best position
        popup?.showUnderneathOf(component)

        // Request focus for search field after popup is shown
        SwingUtilities.invokeLater {
            IdeFocusManager.findInstance().requestFocus(searchField.textEditor, false)
        }
    }

    private inner class FileListCellRenderer : ListCellRenderer<FilePresentation> {
        private val noBorderFocus = BorderFactory.createEmptyBorder(1, 1, 1, 1)

        @NotNull
        override fun getListCellRendererComponent(
            list: JList<out FilePresentation>,
            value: FilePresentation,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean
        ): Component {
            val mainPanel = JPanel(BorderLayout())
            val infoPanel = JPanel(BorderLayout())
            infoPanel.isOpaque = false

            val fileLabel = JBLabel(value.name, value.icon, JBLabel.LEFT)
            fileLabel.border = JBUI.Borders.emptyRight(4)

            val relativePath = value.presentablePath
            val pathLabel = JBLabel(" - $relativePath", JBLabel.LEFT)
            pathLabel.font = UIUtil.getFont(UIUtil.FontSize.SMALL, pathLabel.font)
            pathLabel.foreground = UIUtil.getContextHelpForeground()
            pathLabel.toolTipText = relativePath

            if (value.isRecentFile) {
                fileLabel.foreground = JBColor(0x0087FF, 0x589DF6)
            }

            infoPanel.add(fileLabel, BorderLayout.WEST)
            infoPanel.add(pathLabel, BorderLayout.CENTER)

            mainPanel.add(infoPanel, BorderLayout.CENTER)

            if (isSelected) {
                mainPanel.background = list.selectionBackground
                mainPanel.foreground = list.selectionForeground
            } else {
                mainPanel.background = list.background
                mainPanel.foreground = list.foreground
            }

            mainPanel.border = if (cellHasFocus) {
                UIManager.getBorder("List.focusCellHighlightBorder") ?: noBorderFocus
            } else {
                noBorderFocus
            }

            return mainPanel
        }
    }
}