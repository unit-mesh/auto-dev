/**
 * The MIT License (MIT)
 * <p>
 *     https://github.com/nikolaikopernik/code-complexity-plugin
 *  </p>
 */
package cc.unitmesh.devti.devins.provider.complex

data class ComplexityPoint(
    val complexity: Int,
    val nesting: Int,
    val type: PointType) {

    override fun toString(): String = ". ".repeat(nesting) + "$type + $complexity"
}

enum class PointType {
    LOOP_WHILE,
    LOOP_FOR,
    IF,
    ELSE,
    SWITCH,
    CATCH,
    BREAK,
    CONTINUE,
    LOGICAL_AND,
    LOGICAL_OR,
    RECURSION,
    METHOD,
    UNKNOWN
}
