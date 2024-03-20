package cc.unitmesh.devti.language.compiler.exec

import cc.unitmesh.devti.language.git.GitUtil
import cc.unitmesh.devti.util.parser.Code
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vcs.changes.LocalChangeList

class CommitInsCommand(val myProject: Project, val code: String) : InsCommand {
    override fun execute(): String {
        val commitMsg = Code.parse(code).text

        val changeListManager = ChangeListManager.getInstance(myProject)
        changeListManager.changeLists.forEach {
            val list: LocalChangeList = changeListManager.getChangeList(it.id) ?: return@forEach
            GitUtil.doCommit(myProject, list, commitMsg)
        }

        return "Committing..."
    }
}
