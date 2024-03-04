package cc.unitmesh.devti.counit.model

/**
 * `Tooling` class represents a tooling configuration.
 *
 * @property name The name of the tooling.
 * @property description A short description of what the tooling is used for.
 * @property schema A JSON schema that defines the structure of the tooling configuration.
 * @property examples A list of example configurations that illustrate how to use the tooling.
 */
data class Tooling(
    val name: String,
    val description: String,
    val schema: String,
    val examples: List<String>
)