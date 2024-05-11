package cc.unitmesh.devti.gui.component

import cc.unitmesh.devti.provider.local.JsonTextProvider
import com.intellij.openapi.project.Project
import com.intellij.ui.LanguageTextField

fun JsonLanguageField(myProject: Project?, value: String, placeholder: String, fileName: String) : LanguageTextField {
    return JsonTextProvider.create(myProject, value, placeholder, fileName)
}

