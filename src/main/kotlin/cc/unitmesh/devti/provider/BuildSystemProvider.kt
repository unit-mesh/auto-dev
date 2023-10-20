package cc.unitmesh.devti.provider

/**
 * The `BuildSystemProvider` interface represents a provider for build system information.
 * It provides methods to retrieve the name and version of the build tool being used, as well as the name and version of the programming language being used.
 *
 * This interface is used in conjunction with the [cc.unitmesh.devti.template.DockerfileContext] class.
 */
interface BuildSystemProvider {
    /**
     * Returns the name of the build tool being used.
     *
     * @return The name of the build tool.
     */
    fun buildToolName(): String

    /**
     * Returns the version of the build tool being used.
     *
     * @return The version of the build tool.
     */
    fun buildToolVersion(): String

    /**
     * Returns the name of the programming language being used.
     *
     * @return The name of the programming language.
     */
    fun languageName(): String

    /**
     * Returns the version of the programming language being used.
     *
     * @return The version of the programming language.
     */
    fun languageVersion(): String
}