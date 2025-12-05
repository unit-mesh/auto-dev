package cc.unitmesh.devti.provider

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.serviceContainer.LazyExtensionInstance
import kotlinx.serialization.Serializable

/**
 * Abstract provider for fetching library version information from different package managers.
 * Implementations should handle specific package manager APIs (npm, maven, pypi, etc.).
 */
abstract class LibraryVersionProvider : LazyExtensionInstance<LibraryVersionProvider>() {
    
    /**
     * The type of package manager this provider handles (e.g., "npm", "maven", "pypi")
     */
    abstract val packageType: String
    
    /**
     * Check if this provider can handle the given package type
     */
    open fun canHandle(packageType: String): Boolean {
        return this.packageType.equals(packageType, ignoreCase = true)
    }
    
    /**
     * Fetch version information for a single package
     */
    abstract suspend fun fetchVersion(request: VersionRequest): VersionResult
    

    
    /**
     * Get supported package types by this provider
     */
    open fun getSupportedTypes(): List<String> {
        return listOf(packageType)
    }
    
    companion object {
        val EP_NAME: ExtensionPointName<LibraryVersionProvider> =
            ExtensionPointName.create("cc.unitmesh.libraryVersionProvider")
        
        /**
         * Find provider for the given package type
         */
        fun findProvider(packageType: String): LibraryVersionProvider? {
            return EP_NAME.extensionList.find { it.canHandle(packageType) }
        }
        
        /**
         * Get all available providers
         */
        fun getAllProviders(): List<LibraryVersionProvider> {
            return EP_NAME.extensionList
        }
        
        /**
         * Get all supported package types
         */
        fun getSupportedTypes(): List<String> {
            return EP_NAME.extensionList.flatMap { it.getSupportedTypes() }.distinct()
        }
    }
}

/**
 * Simple request for fetching library version information
 */
@Serializable
data class VersionRequest(
    val name: String,
    val type: String? = null  // Optional, will auto-detect if not provided
)

/**
 * Result of version fetching operation
 */
@Serializable
data class VersionResult(
    val name: String,
    val type: String,
    val version: String? = null,
    val error: String? = null
) {
    val success: Boolean get() = version != null

    companion object {
        fun success(name: String, type: String, version: String): VersionResult {
            return VersionResult(name, type, version, null)
        }

        fun error(name: String, type: String, error: String): VersionResult {
            return VersionResult(name, type, null, error)
        }
    }
}
