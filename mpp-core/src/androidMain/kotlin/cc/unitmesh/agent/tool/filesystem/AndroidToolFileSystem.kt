package cc.unitmesh.agent.tool.filesystem

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import cc.unitmesh.agent.tool.ToolErrorType
import cc.unitmesh.agent.tool.ToolException
import java.io.File
import java.io.FileNotFoundException

/**
 * Android implementation of ToolFileSystem with SAF (Storage Access Framework) support
 * Handles both regular file paths and content:// URIs
 */
class AndroidToolFileSystem(
    private val context: Context,
    private val projectPath: String? = null
) : ToolFileSystem {
    
    private val contentResolver: ContentResolver = context.contentResolver
    
    override fun getProjectPath(): String? = projectPath
    
    override suspend fun readFile(path: String): String? {
        return try {
            if (isContentUri(path)) {
                readFileFromContentUri(path)
            } else {
                readFileFromPath(path)
            }
        } catch (e: Exception) {
            throw ToolException("Failed to read file: $path - ${e.message}", ToolErrorType.FILE_NOT_FOUND, e)
        }
    }
    
    override suspend fun writeFile(path: String, content: String, createDirectories: Boolean) {
        try {
            if (isContentUri(path)) {
                writeFileToContentUri(path, content, createDirectories)
            } else {
                writeFileToPath(path, content, createDirectories)
            }
        } catch (e: Exception) {
            throw ToolException("Failed to write file: $path - ${e.message}", ToolErrorType.FILE_ACCESS_DENIED, e)
        }
    }
    
    override fun exists(path: String): Boolean {
        return try {
            if (isContentUri(path)) {
                existsContentUri(path)
            } else {
                File(resolvePath(path)).exists()
            }
        } catch (e: Exception) {
            false
        }
    }
    
    override fun listFiles(path: String, pattern: String?): List<String> {
        return try {
            if (isContentUri(path)) {
                listFilesFromContentUri(path, pattern)
            } else {
                listFilesFromPath(path, pattern)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    override fun resolvePath(relativePath: String): String {
        if (isContentUri(relativePath) || File(relativePath).isAbsolute) {
            return relativePath
        }
        return if (projectPath != null) {
            File(projectPath, relativePath).absolutePath
        } else {
            File(relativePath).absolutePath
        }
    }
    
    override fun getFileInfo(path: String): FileInfo? {
        return try {
            if (isContentUri(path)) {
                getFileInfoFromContentUri(path)
            } else {
                getFileInfoFromPath(path)
            }
        } catch (e: Exception) {
            null
        }
    }
    
    override fun createDirectory(path: String, createParents: Boolean) {
        try {
            if (isContentUri(path)) {
                createDirectoryInContentUri(path)
            } else {
                val dir = File(resolvePath(path))
                if (createParents) {
                    dir.mkdirs()
                } else {
                    dir.mkdir()
                }
            }
        } catch (e: Exception) {
            throw ToolException("Failed to create directory: $path - ${e.message}", ToolErrorType.FILE_ACCESS_DENIED, e)
        }
    }
    
    override fun delete(path: String, recursive: Boolean) {
        try {
            if (isContentUri(path)) {
                deleteContentUri(path)
            } else {
                val file = File(resolvePath(path))
                if (recursive) {
                    file.deleteRecursively()
                } else {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            throw ToolException("Failed to delete: $path - ${e.message}", ToolErrorType.FILE_ACCESS_DENIED, e)
        }
    }
    
    // Content URI helpers
    
    private fun isContentUri(path: String): Boolean {
        return path.startsWith("content://")
    }
    
    private fun readFileFromContentUri(uriString: String): String? {
        val uri = Uri.parse(uriString)
        return contentResolver.openInputStream(uri)?.use { inputStream ->
            inputStream.bufferedReader().use { it.readText() }
        }
    }
    
    private fun writeFileToContentUri(uriString: String, content: String, createDirectories: Boolean) {
        val uri = Uri.parse(uriString)
        
        // Check if the file exists
        val exists = existsContentUri(uriString)
        
        if (!exists) {
            // Need to create the file
            val parentUri = getParentUri(uri)
            if (parentUri != null) {
                val fileName = getFileNameFromUri(uri)
                val mimeType = getMimeType(fileName)
                
                // Create the file in the parent directory
                val newFileUri = DocumentsContract.createDocument(
                    contentResolver,
                    parentUri,
                    mimeType,
                    fileName
                )
                
                if (newFileUri != null) {
                    // Write content to the newly created file
                    contentResolver.openOutputStream(newFileUri)?.use { outputStream ->
                        outputStream.bufferedWriter().use { it.write(content) }
                    }
                    return
                } else {
                    throw ToolException(
                        "Failed to create file: $fileName",
                        ToolErrorType.FILE_ACCESS_DENIED
                    )
                }
            } else {
                throw ToolException(
                    "Cannot create file - invalid parent URI",
                    ToolErrorType.FILE_ACCESS_DENIED
                )
            }
        }
        
        // File exists, write to it
        contentResolver.openOutputStream(uri, "wt")?.use { outputStream ->
            outputStream.bufferedWriter().use { it.write(content) }
        } ?: throw FileNotFoundException("Cannot open output stream for: $uriString")
    }
    
    private fun existsContentUri(uriString: String): Boolean {
        return try {
            val uri = Uri.parse(uriString)
            val documentFile = DocumentFile.fromSingleUri(context, uri)
            documentFile?.exists() ?: false
        } catch (e: Exception) {
            false
        }
    }
    
    private fun listFilesFromContentUri(uriString: String, pattern: String?): List<String> {
        val uri = Uri.parse(uriString)
        val documentFile = DocumentFile.fromTreeUri(context, uri) ?: return emptyList()
        
        if (!documentFile.isDirectory) {
            return emptyList()
        }
        
        val files = documentFile.listFiles().filter { it.isFile }
        
        return if (pattern != null) {
            val regex = pattern.replace("*", ".*").replace("?", ".").toRegex()
            files.filter { file ->
                file.name?.let { regex.matches(it) } ?: false
            }.map { it.uri.toString() }
        } else {
            files.map { it.uri.toString() }
        }
    }
    
    private fun getFileInfoFromContentUri(uriString: String): FileInfo? {
        val uri = Uri.parse(uriString)
        val documentFile = DocumentFile.fromSingleUri(context, uri) ?: return null
        
        return FileInfo(
            path = uriString,
            isDirectory = documentFile.isDirectory,
            size = documentFile.length(),
            lastModified = documentFile.lastModified(),
            isReadable = documentFile.canRead(),
            isWritable = documentFile.canWrite()
        )
    }
    
    private fun createDirectoryInContentUri(uriString: String) {
        val uri = Uri.parse(uriString)
        val parentUri = getParentUri(uri)
        if (parentUri != null) {
            val dirName = getFileNameFromUri(uri)
            DocumentsContract.createDocument(
                contentResolver,
                parentUri,
                DocumentsContract.Document.MIME_TYPE_DIR,
                dirName
            )
        }
    }
    
    private fun deleteContentUri(uriString: String) {
        val uri = Uri.parse(uriString)
        DocumentsContract.deleteDocument(contentResolver, uri)
    }
    
    private fun getParentUri(uri: Uri): Uri? {
        return try {
            val documentId = DocumentsContract.getDocumentId(uri)
            val authority = uri.authority ?: return null
            
            // For tree URIs, extract parent from document ID
            val pathSegments = documentId.split("/")
            if (pathSegments.size > 1) {
                val parentPath = pathSegments.dropLast(1).joinToString("/")
                val treeUri = DocumentsContract.buildTreeDocumentUri(authority, parentPath)
                DocumentsContract.buildDocumentUriUsingTree(treeUri, parentPath)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    private fun getFileNameFromUri(uri: Uri): String {
        return try {
            val documentId = DocumentsContract.getDocumentId(uri)
            val pathSegments = documentId.split("/")
            pathSegments.lastOrNull() ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }
    
    private fun getMimeType(fileName: String): String {
        return when {
            fileName.endsWith(".txt") -> "text/plain"
            fileName.endsWith(".java") -> "text/x-java"
            fileName.endsWith(".kt") -> "text/x-kotlin"
            fileName.endsWith(".js") -> "text/javascript"
            fileName.endsWith(".json") -> "application/json"
            fileName.endsWith(".xml") -> "text/xml"
            fileName.endsWith(".md") -> "text/markdown"
            else -> "application/octet-stream"
        }
    }
    
    // Regular file path helpers
    
    private fun readFileFromPath(path: String): String? {
        val file = File(resolvePath(path))
        return if (file.exists() && file.isFile) {
            file.readText()
        } else {
            null
        }
    }
    
    private fun writeFileToPath(path: String, content: String, createDirectories: Boolean) {
        val file = File(resolvePath(path))
        
        if (createDirectories) {
            file.parentFile?.mkdirs()
        }
        
        file.writeText(content)
    }
    
    private fun listFilesFromPath(path: String, pattern: String?): List<String> {
        val dir = File(resolvePath(path))
        if (!dir.exists() || !dir.isDirectory) {
            return emptyList()
        }
        
        val files = dir.listFiles()?.filter { it.isFile } ?: return emptyList()
        
        return if (pattern != null) {
            val regex = pattern.replace("*", ".*").replace("?", ".").toRegex()
            files.filter { regex.matches(it.name) }.map { it.absolutePath }
        } else {
            files.map { it.absolutePath }
        }
    }
    
    private fun getFileInfoFromPath(path: String): FileInfo? {
        val file = File(resolvePath(path))
        if (!file.exists()) {
            return null
        }
        
        return FileInfo(
            path = file.absolutePath,
            isDirectory = file.isDirectory,
            size = file.length(),
            lastModified = file.lastModified(),
            isReadable = file.canRead(),
            isWritable = file.canWrite()
        )
    }
}

