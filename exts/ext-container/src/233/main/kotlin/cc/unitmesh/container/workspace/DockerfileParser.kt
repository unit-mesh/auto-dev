// Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package cc.unitmesh.container.workspace

import com.intellij.docker.dockerFile.parser.psi.DockerFileAddOrCopyCommand
import com.intellij.docker.dockerFile.parser.psi.DockerFileCmdCommand
import com.intellij.docker.dockerFile.parser.psi.DockerFileExposeCommand
import com.intellij.docker.dockerFile.parser.psi.DockerFileFromCommand
import com.intellij.docker.dockerFile.parser.psi.DockerFileWorkdirCommand
import com.intellij.docker.dockerFile.parser.psi.DockerPsiCommand
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.source.tree.LeafPsiElement
import java.io.File

class DockerfileParser(private val project: Project) {
    fun parse(virtualFile: VirtualFile): DockerfileDetails? {
        val psiFile = PsiManager.getInstance(project).findFile(virtualFile)!!
        val contextDirectory = virtualFile.parent.path

        val lastFromCommand = psiFile.children.filterIsInstance<DockerFileFromCommand>().lastOrNull() ?: return null
        val commandsAfterLastFrom = psiFile.children.dropWhile { it != lastFromCommand }
        if (commandsAfterLastFrom.isEmpty()) {
            return null
        }

        val command = commandsAfterLastFrom.filterIsInstance<DockerFileCmdCommand>().lastOrNull()?.text?.substringAfter("CMD ")
        val portMappings = commandsAfterLastFrom.filterIsInstance<DockerFileExposeCommand>().mapNotNull {
            it.listChildren().find { child -> (child as? LeafPsiElement)?.elementType?.toString() == "INTEGER_LITERAL" }?.text?.toIntOrNull()
        }

        val copyDirectives = groupByWorkDir(commandsAfterLastFrom).flatMap { (workDir, commands) ->
            commands.filterIsInstance<DockerFileAddOrCopyCommand>()
                .filter { it.copyKeyword != null }
                .mapNotNull { cmd -> cmd.fileOrUrlList.takeIf { it.size == 2 }?.let { it.first().text to it.last().text } }
                .map { (rawLocal, rawRemote) ->
                    val local = if (rawLocal.startsWith("/") || rawLocal.startsWith(File.separatorChar)) {
                        rawLocal
                    } else {
                        "${contextDirectory.normalizeDirectory(true)}$rawLocal"
                    }
                    val remote = if (rawRemote.startsWith("/") || workDir == null) {
                        rawRemote
                    } else {
                        "${workDir.normalizeDirectory()}$rawRemote"
                    }
                    CopyDirective(local, remote)
                }
        }

        return DockerfileDetails(command, portMappings, copyDirectives)
    }

    private fun String.normalizeDirectory(matchPlatform: Boolean = false): String {
        val ch = if (matchPlatform) File.separatorChar else '/'
        return "${trimEnd(ch)}$ch"
    }

    private fun groupByWorkDir(commands: List<PsiElement>): List<Pair<String?, List<DockerPsiCommand>>> {
        val list = mutableListOf<Pair<String?, List<DockerPsiCommand>>>()
        var workDir: String? = null
        val elements = mutableListOf<DockerPsiCommand>()
        commands.forEach {
            when (it) {
                is DockerFileWorkdirCommand -> {
                    if (elements.isNotEmpty()) {
                        list.add(workDir to elements.toList())
                        elements.clear()
                    }
                    workDir = it.fileOrUrlList.first().text
                }
                is DockerPsiCommand -> elements.add(it)
            }
        }
        if (elements.isNotEmpty()) {
            list.add(workDir to elements.toList())
        }
        return list
    }

    private fun PsiElement.listChildren(): List<PsiElement> {
        var child: PsiElement? = firstChild ?: return emptyList()
        val children = mutableListOf<PsiElement>()
        while (child != null) {
            children.add(child)
            child = child.nextSibling
        }
        return children.toList()
    }
}

data class DockerfileDetails(val command: String?, val exposePorts: List<Int>, val copyDirectives: List<CopyDirective>)
data class CopyDirective(val from: String, val to: String)