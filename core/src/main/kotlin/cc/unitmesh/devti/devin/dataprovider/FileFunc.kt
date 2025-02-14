package cc.unitmesh.devti.devin.dataprovider

import com.intellij.icons.AllIcons
import javax.swing.Icon

enum class FileFunc(val funcName: String, val description: String, val icon: Icon) {
    Regex("regex", "Read the content of a file by regex", AllIcons.Actions.Regex),

    ;

    companion object {
        fun all(): List<FileFunc> {
            return values().toList()
        }

        fun fromString(funcName: String): FileFunc? {
            return values().find { it.funcName == funcName }
        }
    }
}