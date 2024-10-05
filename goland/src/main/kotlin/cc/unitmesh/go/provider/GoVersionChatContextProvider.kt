package cc.unitmesh.go.provider

import cc.unitmesh.devti.provider.context.ChatContextItem
import cc.unitmesh.devti.provider.context.ChatContextProvider
import cc.unitmesh.devti.provider.context.ChatCreationContext
import com.goide.psi.GoFile
import com.goide.sdk.GoSdkService
import com.goide.util.GoUtil
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import java.lang.reflect.Method

class GoVersionChatContextProvider : ChatContextProvider {
    override fun isApplicable(project: Project, creationContext: ChatCreationContext): Boolean {
        return creationContext.sourceFile is GoFile
    }

    override suspend fun collect(project: Project, creationContext: ChatCreationContext): List<ChatContextItem> {
        val sourceFile = creationContext.sourceFile ?: return emptyList()

        return ReadAction.compute<List<ChatContextItem>, Throwable> {
            val goVersion = GoSdkService.getInstance(project).getSdk(GoUtil.module(sourceFile)).version
            val targetVersion = getGoVersion(sourceFile)

            listOf(
                ChatContextItem(
                    GoVersionChatContextProvider::class,
                    "Go Version: $goVersion, Target Version: $targetVersion"
                )
            )
        }
    }

    private fun getGoVersion(sourceFile: PsiFile): String {
        return try {
            val clazz = Class.forName("com.goide.sdk.GoTargetSdkVersionProvider")
            val method: Method = clazz.getMethod("getTargetGoSdkVersion", PsiElement::class.java)
            val result = method.invoke(null, sourceFile)
            result?.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } ?: ""
    }

}

