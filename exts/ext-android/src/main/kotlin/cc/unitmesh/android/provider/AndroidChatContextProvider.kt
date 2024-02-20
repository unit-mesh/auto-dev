package cc.unitmesh.android.provider

import cc.unitmesh.android.util.AdSdkFinder
import cc.unitmesh.devti.provider.context.ChatContextItem
import cc.unitmesh.devti.provider.context.ChatContextProvider
import cc.unitmesh.devti.provider.context.ChatCreationContext
import com.android.tools.idea.model.AndroidModel
import com.android.tools.idea.projectsystem.getAndroidFacets
import com.intellij.openapi.project.Project
import org.jetbrains.android.util.AndroidUtils

class AndroidChatContextProvider : ChatContextProvider {
    override fun isApplicable(project: Project, creationContext: ChatCreationContext): Boolean {
        return AndroidUtils.hasAndroidFacets(project)
    }

    override suspend fun collect(project: Project, creationContext: ChatCreationContext): List<ChatContextItem> {
        var text = "This project is a Mobile Android project."
        val sdkVersion = getProjectAndroidTargetSdkVersion(project)
        if (sdkVersion != null) {
            text += "Android SDK target version is $sdkVersion."
        }

        return listOf(ChatContextItem(AndroidChatContextProvider::class, text))
    }

    private fun getProjectAndroidTargetSdkVersion(project: Project): Int? {
        val maxTargetSdkVersion = project.getAndroidFacets()
            .mapNotNull {
                AndroidModel.get(it)?.targetSdkVersion?.apiLevel
            }.maxOrNull()

        return maxTargetSdkVersion ?: let { AdSdkFinder.getSdkVersion() }
    }
}