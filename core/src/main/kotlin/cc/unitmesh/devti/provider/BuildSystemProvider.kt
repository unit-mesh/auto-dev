package cc.unitmesh.devti.provider

import cc.unitmesh.devti.template.context.DockerfileContext
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.serviceContainer.LazyExtensionInstance
import com.intellij.util.xmlb.annotations.Attribute

/**
 * The `BuildSystemProvider` interface represents a provider for build system information.
 * It provides methods to retrieve the name and version of the build tool being used, as well as the name and version of the programming language being used.
 *
 * This interface is used in conjunction with the [cc.unitmesh.devti.template.DockerfileContext] class.
 */
abstract class BuildSystemProvider : LazyExtensionInstance<BuildSystemProvider>() {
    abstract fun collect(project: Project): DockerfileContext?

    /**
     * the DeclarePackageFile means `build.gradle`, `pom.xml`, `build.sbt`, `package.json` etc.
     */
    abstract fun isDeclarePackageFile(filename: String): Boolean

    /**
     * For PsiFile only for resolve in Sketch and Bridge mode
     * the BuildFilePsiFile means `build.gradle`, `pom.xml`, `build.sbt`, `package.json` etc.
     */
    open fun collectDependencies(project: Project, buildFilePsi: PsiFile): List<DevPackage> {
        return emptyList()
    }

    @Attribute("implementationClass")
    var implementationClass: String? = null

    override fun getImplementationClassName(): String? {
        return implementationClass
    }

    companion object {
        val EP_NAME: ExtensionPointName<BuildSystemProvider> =
            ExtensionPointName.create("cc.unitmesh.buildSystemProvider")

        fun guess(project: Project): List<DockerfileContext> {
            return EP_NAME.extensionList.mapNotNull {
                it.collect(project)
            }
        }

        fun isDeclarePackageFile(filename: String?): Boolean {
            if (filename == null) return false

            return EP_NAME.extensionList.any {
                it.isDeclarePackageFile(filename)
            }
        }
    }
}

data class DevPackage(
    val type: String,
    val namespace: String? = null,
    val name: String,
    val version: String,
    val qualifiers: String? = null,
    val subpath: String? = null,
    val releaseDate: java.util.Date? = null
) {
    val coordinates: String = "$type:$namespace:$name:$version:$qualifiers:$subpath"
    val humanReadableCoordinates: String = buildString {
        append(type)
        namespace?.let { append(":$it") }
        append(":$name:$version")
        qualifiers?.let { append(":$it") }
        subpath?.let { append(":$it") }
    }
    val searchCoordinates: String = "$namespace:$name:$version"
    val searchKey: String = calculateContentsHashSha256("$type:$searchCoordinates")

    fun calculateContentsHashSha256(vararg content: Any?): String {
        val messageDigest = java.security.MessageDigest.getInstance("SHA-256")
        val joinedString = content.joinToString(":") { it.toString() }
        val bytes = joinedString.toByteArray()
        val digest = messageDigest.digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }
}
