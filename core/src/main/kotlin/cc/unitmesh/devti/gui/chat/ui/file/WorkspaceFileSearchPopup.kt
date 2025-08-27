package cc.unitmesh.devti.gui.chat.ui.file

import cc.unitmesh.devti.util.canBeAdded
import com.intellij.openapi.Disposable
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
) : Disposable {
    companion object {
        private val LOG = Logger.getInstance(WorkspaceFileSearchPopup::class.java)
        private const val MAX_RECENT_FILES = 30
        private const val SEARCH_DELAY_MS = 300
        private const val BATCH_SIZE = 50
        private const val MAX_FILES_TO_SCAN = 10000
        private const val PROGRESS_UPDATE_INTERVAL = 100
        private const val SCROLL_THRESHOLD = 10
        private const val MIN_QUERY_LENGTH = 2
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
    private val searchAlarm = Alarm(Alarm.ThreadToUse.POOLED_THREAD, this)

    // File storage
    private val recentFiles = mutableListOf<FilePresentation>()
    private val projectFiles = mutableListOf<FilePresentation>()

    // Loading state flags
    private var isLoadingFiles = false
    private var hasLoadedAllFiles = false
    private var currentSearchQuery = ""

    init {
        setupUI()
        loadRecentFiles()
    }

    private fun setupUI() {
        configureSearchField()
        configureFileList()
        setupLayout()
    }

    private fun configureSearchField() {
        searchField.textEditor.addKeyListener(object : KeyAdapter() {
            override fun keyReleased(e: KeyEvent) {
                if (e.keyCode == KeyEvent.VK_DOWN && fileListModel.size > 0) {
                    transferFocusToFileList()
                    return
                }

                // Use delayed search to avoid triggering search on every keystroke
                val query = searchField.text.trim()
                scheduleSearch(query)
            }
        })
    }

    private fun configureFileList() {
        fileList.addKeyListener(createFileListKeyListener())
        fileList.addMouseListener(createFileListMouseListener())
    }

    private fun createFileListKeyListener() = object : KeyAdapter() {
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
    }

    private fun createFileListMouseListener() = object : MouseAdapter() {
        override fun mouseClicked(e: MouseEvent) {
            if (e.clickCount == 2) {
                selectFiles()
            }
        }
    }

    private fun setupLayout() {
        val scrollPane = createScrollPane()
        val statusPanel = createStatusPanel()

        contentPanel.apply {
            border = JBUI.Borders.empty()
            add(searchField, BorderLayout.NORTH)
            add(scrollPane, BorderLayout.CENTER)
            add(statusPanel, BorderLayout.SOUTH)
            preferredSize = minPopupSize
        }
    }

    private fun createScrollPane(): JBScrollPane {
        val scrollPane = JBScrollPane(fileList)
        scrollPane.verticalScrollBar.addAdjustmentListener { e ->
            val scrollBar = e.adjustable
            // Load more files when scrolled near bottom
            if (shouldLoadMoreFiles(scrollBar)) {
                loadMoreProjectFiles()
            }
        }
        return scrollPane
    }

    private fun createStatusPanel(): JPanel {
        val statusPanel = JPanel(BorderLayout())
        statusPanel.add(loadingLabel, BorderLayout.CENTER)
        return statusPanel
    }

    private fun transferFocusToFileList() {
        fileList.requestFocus()
        fileList.selectedIndex = 0
    }

    private fun shouldLoadMoreFiles(scrollBar: java.awt.Adjustable): Boolean {
        return !isLoadingFiles && !hasLoadedAllFiles &&
                scrollBar.value + scrollBar.visibleAmount >= scrollBar.maximum - SCROLL_THRESHOLD
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

        when {
            query.isBlank() -> showRecentAndLoadedFiles()
            query.length >= MIN_QUERY_LENGTH -> searchFilesWithQuery(query)
            else -> showRecentFiles()
        }
    }

    private fun showRecentAndLoadedFiles() {
        val filesToShow = buildFileListForDisplay()
        displayFiles(filesToShow)

        // Start loading project files if not already started
        if (shouldStartLoadingProjectFiles()) {
            startLoadingProjectFiles()
        }

        selectFirstItemIfAvailable()
    }

    private fun buildFileListForDisplay(): List<FilePresentation> {
        val filesToShow = ArrayList<FilePresentation>()
        filesToShow.addAll(recentFiles)

        // Add loaded project files (excluding already shown recent files)
        val recentPaths = recentFiles.map { it.path }.toSet()
        filesToShow.addAll(projectFiles.filter { it.path !in recentPaths }.take(BATCH_SIZE))

        return filesToShow.sortedWith(createFileSortComparator())
    }

    private fun displayFiles(files: List<FilePresentation>) {
        files.forEach { fileListModel.addElement(it) }
    }

    private fun createFileSortComparator(): Comparator<FilePresentation> {
        return compareBy<FilePresentation> { !it.isRecentFile }.thenBy { it.name }
    }

    private fun shouldStartLoadingProjectFiles(): Boolean {
        return projectFiles.isEmpty() && !isLoadingFiles && !hasLoadedAllFiles
    }

    private fun showRecentFiles() {
        recentFiles.forEach { fileListModel.addElement(it) }
        selectFirstItemIfAvailable()
    }

    private fun searchFilesWithQuery(query: String) {
        val matchingLoaded = searchLoadedFiles(query)
        val additionalFiles = searchIndexedFiles(query, matchingLoaded)

        val allResults = combineAndSortResults(matchingLoaded, additionalFiles)
        displaySearchResults(allResults)

        selectFirstItemIfAvailable()
    }

    private fun searchLoadedFiles(query: String): List<FilePresentation> {
        val matchingLoaded = ArrayList<FilePresentation>()

        // Search recent files
        matchingLoaded.addAll(recentFiles.filter { fileMatchesQuery(it, query) })

        // Search loaded project files (excluding recent files)
        val recentPaths = recentFiles.map { it.path }.toSet()
        matchingLoaded.addAll(projectFiles.filter {
            it.path !in recentPaths && fileMatchesQuery(it, query)
        })

        return matchingLoaded
    }

    private fun fileMatchesQuery(file: FilePresentation, query: String): Boolean {
        return file.name.contains(query, ignoreCase = true) ||
               file.path.contains(query, ignoreCase = true)
    }

    private fun searchIndexedFiles(query: String, existingFiles: List<FilePresentation>): List<FilePresentation> {
        val scope = GlobalSearchScope.projectScope(project)
        val additionalFiles = mutableListOf<FilePresentation>()
        val existingPaths = existingFiles.map { it.path }.toSet()

        ApplicationManager.getApplication().runReadAction {
            FilenameIndex.processFilesByName(query, false, scope) { file ->
                if (file.canBeAdded(project) && file.path !in existingPaths) {
                    additionalFiles.add(FilePresentation.from(project, file))
                }
                true
            }
        }

        return additionalFiles
    }

    private fun combineAndSortResults(
        matchingLoaded: List<FilePresentation>,
        additionalFiles: List<FilePresentation>
    ): List<FilePresentation> {
        val allResults = ArrayList<FilePresentation>()
        allResults.addAll(matchingLoaded)
        allResults.addAll(additionalFiles)

        return allResults.sortedWith(createFileSortComparator())
    }

    private fun displaySearchResults(results: List<FilePresentation>) {
        results.take(BATCH_SIZE).forEach { fileListModel.addElement(it) }
    }

    private fun loadRecentFiles() {
        recentFiles.clear()

        // Load recently opened files
        val fileList = EditorHistoryManager.getInstance(project).fileList
        fileList.take(MAX_RECENT_FILES)
            .filter { it.canBeAdded(project) }
            .forEach { file ->
                val presentation = FilePresentation.from(project, file)
                presentation.isRecentFile = true
                recentFiles.add(presentation)
            }

        // Initially display recent files
        showRecentFiles()
    }

    private fun startLoadingProjectFiles() {
        if (isLoadingFiles || hasLoadedAllFiles) return

        setLoadingState(true)

        // Load project files in background
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Loading Files", false) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    loadMoreProjectFilesInternal(indicator)
                } finally {
                    ApplicationManager.getApplication().invokeLater({
                        setLoadingState(false)
                        updateUIAfterLoading()
                    }, ModalityState.any())
                }
            }
        })
    }

    private fun setLoadingState(loading: Boolean) {
        isLoadingFiles = loading
        loadingLabel.isVisible = loading
    }

    private fun updateUIAfterLoading() {
        // Update search results if there's a query, otherwise show recent and loaded files
        if (currentSearchQuery.isNotBlank()) {
            performSearch(currentSearchQuery)
        } else if (shouldShowRecentAndLoadedFiles()) {
            showRecentAndLoadedFiles()
        }
    }

    private fun shouldShowRecentAndLoadedFiles(): Boolean {
        return fileListModel.size == 0 || fileListModel.size == recentFiles.size
    }

    private fun loadMoreProjectFiles() {
        if (isLoadingFiles || hasLoadedAllFiles) return

        setLoadingState(true)

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Loading More Files", false) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    loadMoreProjectFilesInternal(indicator)
                } finally {
                    ApplicationManager.getApplication().invokeLater({
                        setLoadingState(false)
                        updateUIAfterLoadingMore()
                    }, ModalityState.any())
                }
            }
        })
    }

    private fun updateUIAfterLoadingMore() {
        if (currentSearchQuery.isNotBlank()) {
            performSearch(currentSearchQuery)
        } else {
            addNewlyLoadedFilesToList()
        }
    }

    private fun addNewlyLoadedFilesToList() {
        val currentPaths = getCurrentDisplayedPaths()
        val recentPaths = recentFiles.map { it.path }.toSet()

        projectFiles
            .filter { it.path !in recentPaths && it.path !in currentPaths }
            .take(BATCH_SIZE)
            .forEach { fileListModel.addElement(it) }
    }

    private fun getCurrentDisplayedPaths(): Set<String> {
        val currentSize = fileListModel.size
        return (0 until currentSize).mapNotNull {
            fileListModel.getElementAt(it)?.path
        }.toSet()
    }

    private fun loadMoreProjectFilesInternal(indicator: ProgressIndicator) {
        indicator.isIndeterminate = false

        val loadedPaths = getAllLoadedPaths()
        val newFiles = mutableListOf<FilePresentation>()
        var count = 0

        // Use project file index to iterate through files
        ProjectFileIndex.getInstance(project).iterateContent { file ->
            if (indicator.isCanceled) return@iterateContent false

            count++
            updateProgressIndicator(indicator, count)

            if (shouldAddFile(file, loadedPaths)) {
                newFiles.add(FilePresentation.from(project, file))

                // Stop if we've collected enough files for this batch
                if (newFiles.size >= BATCH_SIZE) {
                    return@iterateContent false
                }
            }

            // Stop scanning if we've processed too many files
            if (count > MAX_FILES_TO_SCAN) {
                return@iterateContent false
            }

            true
        }

        updateProjectFilesState(newFiles, count)
        LOG.info("Loaded ${newFiles.size} more files. Total loaded: ${projectFiles.size}")
    }

    private fun getAllLoadedPaths(): Set<String> {
        return (recentFiles + projectFiles).map { it.path }.toSet()
    }

    private fun updateProgressIndicator(indicator: ProgressIndicator, count: Int) {
        if (count % PROGRESS_UPDATE_INTERVAL == 0) {
            indicator.fraction = count.toDouble() / MAX_FILES_TO_SCAN.toDouble()
            indicator.text = "Scanned $count files..."
        }
    }

    private fun shouldAddFile(file: VirtualFile, loadedPaths: Set<String>): Boolean {
        val fileIndex = ProjectFileIndex.getInstance(project)

        // Use new high-performance gitignore engine for ignore checking
        val isIgnored = try {
            cc.unitmesh.devti.vcs.gitignore.GitIgnoreUtil.isIgnored(project, file)
        } catch (e: Exception) {
            // Fallback to original ignore checking
            fileIndex.isUnderIgnored(file)
        }

        return file.canBeAdded(project) &&
                !isIgnored &&
                fileIndex.isInContent(file) &&
                file.path !in loadedPaths
    }

    private fun updateProjectFilesState(newFiles: List<FilePresentation>, count: Int) {
        synchronized(projectFiles) {
            projectFiles.addAll(newFiles)
            hasLoadedAllFiles = newFiles.size < BATCH_SIZE || count > MAX_FILES_TO_SCAN
        }
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
        popup = createPopup()
        popup?.addListener(createPopupListener())
        popup?.showUnderneathOf(component)

        // Request focus for search field after popup is shown
        SwingUtilities.invokeLater {
            IdeFocusManager.findInstance().requestFocus(searchField.textEditor, false)
        }
    }

    private fun createPopup(): JBPopup {
        return JBPopupFactory.getInstance()
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
    }

    private fun createPopupListener() = object : JBPopupListener {
        override fun onClosed(event: LightweightWindowEvent) {
            cleanupResources()
        }
    }

    private fun cleanupResources() {
        // Cancel all background tasks
        searchAlarm.cancelAllRequests()

        // Clear resources
        recentFiles.clear()
        projectFiles.clear()
        fileListModel.clear()

        isLoadingFiles = false
        hasLoadedAllFiles = false
    }

    override fun dispose() {
        cleanupResources()
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