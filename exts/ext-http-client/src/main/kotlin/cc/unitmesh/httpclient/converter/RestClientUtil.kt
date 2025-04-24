package cc.unitmesh.httpclient.converter

import com.intellij.httpClient.execution.RestClientRequest
import com.intellij.httpClient.execution.auth.HttpClientAuthData
import com.intellij.httpClient.execution.auth.HttpRequestAuthCredentials
import com.intellij.httpClient.execution.auth.HttpRequestAuthScope
import com.intellij.httpClient.execution.auth.HttpRequestCommonAuthSchemes
import com.intellij.httpClient.http.request.*
import com.intellij.httpClient.http.request.psi.impl.HttpRequestPsiImplUtil
import com.intellij.ide.scratch.ScratchFileService
import com.intellij.ide.scratch.ScratchRootType
import com.intellij.openapi.command.UndoConfirmationPolicy
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import com.intellij.ui.UIBundle
import com.intellij.util.PathUtil
import com.oracle.truffle.js.runtime.objects.DefaultESModuleLoader
import java.io.IOException
import java.net.URL

fun RestClientRequest.buildFullUrl(): URL {
    val url = URL(getFullUri(this.url, this))
    if (url.userInfo == null) {
        return url
    }
    val userInfo = url.userInfo ?: throw IllegalStateException("getUserInfo(...) returned null")
    val usernameAndPassword = userInfo.split(":")
    val httpRequestAuthScope = HttpRequestAuthScope(HttpRequestCommonAuthSchemes.BASIC)
    val username = usernameAndPassword[0]
    val password = usernameAndPassword.getOrNull(1) ?: ""
    this.authData =
        HttpClientAuthData(httpRequestAuthScope, HttpRequestAuthCredentials.UsernamePassword(username, password))

    val fullUrl = buildString {
        url.protocol?.let {
            append(it).append(HttpRequestPsiImplUtil.SCHEME_SEPARATOR)
        }
        url.host?.let {
            append(it)
        }
        val port = url.port.takeIf { it > 0 }
        port?.let {
            append(":").append(it)
        }
        url.path?.let {
            append(DefaultESModuleLoader.SLASH).append(it)
        }
    }

    return URL(fullUrl)
}

fun getFullUri(uri: String, request: RestClientRequest): String {
    var uriStr = uri
    if (request.parametersEnabled) {
        val query = request.createQueryString()
        if (StringUtil.isNotEmpty(query)) {
            uriStr = uriStr + (if (uriStr.contains("?")) "&" else "?") + query
        }
    }

    return uriStr
}


fun createAndOpenScratchFile(project: Project, request: RestClientRequest, comment: String?) {
    val fileName = PathUtil.makeFileName("rest-api", HttpRequestFileType.INSTANCE.defaultExtension)
    try {
        WriteCommandAction.writeCommandAction(project).withName("Create HTTP Request scratch file")
            .withGlobalUndo()
            .shouldRecordActionForActiveDocument(false)
            .withUndoConfirmationPolicy(UndoConfirmationPolicy.REQUEST_CONFIRMATION)
            .compute<NavigatablePsiElement, Exception> {
                val convertedRequest: String
                val fileService = ScratchFileService.getInstance()
                try {
                    val file = fileService.findFile(
                        ScratchRootType.getInstance(),
                        fileName,
                        ScratchFileService.Option.create_new_always
                    )
                    fileService.scratchesMapping.setMapping(file, HttpRequestLanguage.INSTANCE)
                    val psiFile = PsiManager.getInstance(project).findFile(file) as? HttpRequestPsiFile
                        ?: throw Exception("Failed to create HTTP Request scratch file")

                    val manager = PsiDocumentManager.getInstance(project)
                    val document = manager.getDocument(psiFile)
                        ?: throw Exception("Created HTTP Request scratch file is invalid")

                    convertedRequest = if (comment != null) {
                        comment + HttpRequestPsiConverter.toPsiHttpRequest(request)
                    } else {
                        HttpRequestPsiConverter.toPsiHttpRequest(request)
                    }

                    document.insertString(document.textLength, convertedRequest)
                    manager.commitDocument(document)
                    val updated = HttpRequestPsiUtils.getRequestBlocks(psiFile)
                    if (updated.isNotEmpty()) {
                        return@compute updated[updated.size - 1]
                    }

                    return@compute psiFile
                } catch (e: IOException) {
                    throw Exception("Could not create file: $e.")
                }
            }?.navigate(true)
    } catch (e: Exception) {
        Messages.showErrorDialog(project, e.message, UIBundle.message("error.dialog.title", *arrayOfNulls(0)))
    }
}