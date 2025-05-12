package cc.unitmesh.devti.language.ast

import cc.unitmesh.devti.language.ast.action.PatternActionFunc

/**
 * The `PatternActionTransform` class is a utility class in Kotlin that is used to perform various actions based on a provided pattern and action.
 * The class takes a pattern of type `String` and an action of type `PatternAction` as parameters.
 *
 * @property variable This property represents the pattern to be used for the transformation.
 * @property patternActionFuncs This property represents the action to be performed on the input.
 *
 * @constructor This constructor creates a new instance of `PatternActionTransform` with the specified pattern and action.
 *
 * The `execute` function is a member function of this class which is used to perform the action on the input and return the result as a `String`.
 *
 * @param variable This parameter represents the input on which the action is to be performed.
 * @return The result of the action performed on the input as a `String`.
 *
 * The `execute` function iterates over each action in `patternActionFuncs` and performs the corresponding action on the input.
 * The result of each action is stored in the `result` variable which is initially set to the input.
 * The type of action to be performed is determined using a `when` statement that checks the type of each action.
 * The result of the `execute` function is the final value of the `result` variable converted to a `String`.
 *
 * @see PatternAction
 */
class VariableTransform(
    val variable: String,
    val pattern: String,
    val patternActionFuncs: List<PatternActionFunc>,
    val isQueryStatement: Boolean = false
) {
}