package cc.unitmesh.devti.gui.chat.ui.file

import cc.unitmesh.devti.completion.AutoDevInputLookupManagerListener
import cc.unitmesh.devti.gui.chat.ui.AutoDevInput
import cc.unitmesh.devti.provider.RelatedClassesProvider
import com.intellij.codeInsight.lookup.LookupManagerListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import java.awt.BorderLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.ListSelectionModel

/**
 * Manages file operations and workspace functionality
 */
class RelatedFileWorkspaceManager(
    private val project: Project,
    private val disposable: Disposable?
) {
    // File management components
    private val relatedFileListViewModel = RelatedFileListViewModel(project)
    private val elementsList = JBList(relatedFileListViewModel.getListModel())
    lateinit var workspaceFilePanel: WorkspaceFilePanel
        private set

    fun initialize(input: AutoDevInput): JPanel {
        workspaceFilePanel = WorkspaceFilePanel(project)
        setupElementsList()
        setupEditorListener()
        setupRelatedListener()

        // Initialize with current file
        val currentFile = FileEditorManager.getInstance(project).selectedFiles.firstOrNull()
        currentFile?.let {
            relatedFileListViewModel.addFileIfAbsent(currentFile, first = true)
        }

        return createHeaderPanel(input)
    }

    private fun setupElementsList() {
        elementsList.selectionMode = ListSelectionModel.SINGLE_SELECTION
        elementsList.layoutOrientation = JList.HORIZONTAL_WRAP
        elementsList.visibleRowCount = 2
        elementsList.cellRenderer = RelatedFileListCellRenderer(project)
        elementsList.setEmptyText("")

        elementsList.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                calculateRelativeFile(e)
            }
        })
    }

    private fun setupEditorListener() {
        project.messageBus.connect(disposable!!).subscribe(
            FileEditorManagerListener.FILE_EDITOR_MANAGER,
            object : FileEditorManagerListener {
                override fun selectionChanged(event: FileEditorManagerEvent) {
                    val file = event.newFile ?: return
                    ApplicationManager.getApplication().invokeLater {
                        relatedFileListViewModel.addFileIfAbsent(file, true)
                    }
                }
            }
        )
    }

    private fun setupRelatedListener() {
        project.messageBus.connect(disposable!!)
            .subscribe(LookupManagerListener.TOPIC, AutoDevInputLookupManagerListener(project) {
                ApplicationManager.getApplication().invokeLater {
                    val relatedElements = RelatedClassesProvider.Companion.provide(it.language)?.lookupIO(it)
                    updateElements(relatedElements)
                }
            })
    }

    private fun calculateRelativeFile(e: MouseEvent) {
        val list = e.source as JBList<*>
        val index = list.locationToIndex(e.point)
        if (index == -1) return

        val wrapper = relatedFileListViewModel.getListModel().getElementAt(index)
        val cellBounds = list.getCellBounds(index, index)

        val actionType = relatedFileListViewModel.determineFileAction(wrapper, e.point, cellBounds)
        val actionPerformed = relatedFileListViewModel.handleFileAction(wrapper, actionType) { vfile, relativePath ->
            if (relativePath != null) {
                getWorkspacePanel().addFileToWorkspace(vfile)
                ApplicationManager.getApplication().invokeLater {
                    if (!vfile.isValid) return@invokeLater
                    val psiFile = PsiManager.getInstance(project).findFile(vfile) ?: return@invokeLater
                    val relatedElements = RelatedClassesProvider.Companion.provide(psiFile.language)?.lookupIO(psiFile)
                    updateElements(relatedElements)
                }
            }
        }

        if (!actionPerformed) {
            list.clearSelection()
        }
    }

    private fun updateElements(elements: List<PsiElement>?) {
        elements?.forEach { relatedFileListViewModel.addFileIfAbsent(it.containingFile.virtualFile) }
    }

    private fun createHeaderPanel(input: AutoDevInput): JPanel {
        val scrollPane = JBScrollPane(elementsList)
        scrollPane.verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        scrollPane.horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED

        val toolbar = WorkspaceFileToolbar.createToolbar(project, relatedFileListViewModel, input)

        val headerPanel = JPanel(BorderLayout())
        headerPanel.add(toolbar, BorderLayout.NORTH)
        headerPanel.add(scrollPane, BorderLayout.CENTER)

        return headerPanel
    }

    fun renderText(): String {
        relatedFileListViewModel.clearAllFiles()
        val files = workspaceFilePanel.getAllFilesFormat()
        workspaceFilePanel.clear()
        return files
    }

    fun clearWorkspace() {
        workspaceFilePanel.clear()
    }

    fun getWorkspacePanel(): WorkspaceFilePanel = workspaceFilePanel

    fun getRelatedFileListViewModel(): RelatedFileListViewModel = relatedFileListViewModel
}