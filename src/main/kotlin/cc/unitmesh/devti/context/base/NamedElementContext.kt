package cc.unitmesh.devti.context.base

import com.intellij.psi.PsiElement

/**
 * The `NamedElementContext` class represents a context for a named element in Kotlin language.
 * It is an abstract class that provides information about the root element, text, and name of the element.
 *
 * @property root The root element of the context.
 * @property text The text representation of the context.
 * @property name The name of the element in the context.
 *
 * @see LLMCodeContext
 */
abstract class NamedElementContext(open val root: PsiElement, open val text: String?, open val name: String?) :
    LLMCodeContext {

    /**
     * Formats the named element context into a string representation.
     * In [cc.unitmesh.devti.context.ClassContext], the formatted string representation will be like UML.For example:
     * ```uml
     * 'package: cc.unitmesh.untitled.demo.controller.UserController
     * '@RestController, @RequestMapping("/user")
     * class UserController {
     *
     *   + @GetMapping     public UserDTO getUsers()
     * }
     * ```
     * In [cc.unitmesh.devti.context.MethodContext],
     * the formatted string representation will be just the method signature.For Example
     * ```bash
     * path: /src/test.go
     * language: Go
     * fun name: f3
     * fun signature: (float64, float64, float64)
     * ```
     *
     * In [cc.unitmesh.devti.context.VariableContext], the formatted string representation will be like:
     * ```bash
     * var name: content
     * var method name: format
     * var class name: NamedElementContext
     * ```
     * @return The formatted string representation of the named element context.
     */
    override fun format(): String = ""
}
