package cc.unitmesh.go.provider

import cc.unitmesh.devti.provider.context.ChatContextItem
import cc.unitmesh.devti.provider.context.ChatContextProvider
import cc.unitmesh.devti.provider.context.ChatCreationContext
import com.goide.psi.GoFile
import com.goide.sdk.GoSdkService
import com.goide.sdk.GoTargetSdkVersionProvider
import com.goide.util.GoUtil
import com.intellij.openapi.project.Project

class GoVersionChatContextProvider : ChatContextProvider {
    override fun isApplicable(project: Project, creationContext: ChatCreationContext): Boolean {
        return creationContext.sourceFile is GoFile
    }

    override suspend fun collect(project: Project, creationContext: ChatCreationContext): List<ChatContextItem> {
        val sourceFile = creationContext.sourceFile ?: return emptyList()

        val goVersion = GoSdkService.getInstance(project).getSdk(GoUtil.module(sourceFile)).version
        val targetVersion = GoTargetSdkVersionProvider.getTargetGoSdkVersion(sourceFile).toString()


        return listOf(
            ChatContextItem(
                GoVersionChatContextProvider::class,
                "Go Version: $goVersion, Target Version: $targetVersion"
            )
        )
    }
}

