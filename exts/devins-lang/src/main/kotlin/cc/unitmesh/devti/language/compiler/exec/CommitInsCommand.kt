package cc.unitmesh.devti.language.compiler.exec

import cc.unitmesh.devti.language.git.GitUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vcs.changes.LocalChangeList

class CommitInsCommand(val myProject: Project, val commitMsg: String) : InsCommand {
    override fun execute(): String {
        val changeListManager = ChangeListManager.getInstance(myProject)
        changeListManager.changeLists.forEach {
            val list: LocalChangeList = changeListManager.getChangeList(it.id) ?: return@forEach
            GitUtil.doCommit(myProject, list, commitMsg)
        }

        return "Committing..."
    }
}
