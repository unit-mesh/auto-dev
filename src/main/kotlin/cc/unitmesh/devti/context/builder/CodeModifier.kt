package cc.unitmesh.devti.context.builder

import com.intellij.lang.Language
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

/**
 * The `CodeModifier` interface provides methods for modifying code in a given project.
 * It allows for inserting test code, methods, and classes into source files.
 */
interface CodeModifier {
    /**
     * Checks if the given code language is applicable.
     *
     * @param language The language to check.
     * @return True if the language is applicable, false otherwise.
     */
    fun isApplicable(language: Language): Boolean
    /**
     * Inserts the provided test code into the specified source file in the given project.
     *
     * @param sourceFile The virtual file representing the source file where the test code will be inserted.
     * @param project The project in which the source file belongs.
     * @param code The test code to be inserted into the source file.
     * @return True if the test code was successfully inserted, false otherwise.
     */
    fun insertTestCode(sourceFile: VirtualFile, project: Project, code: String): Boolean
    /**
     * Inserts a method into the specified source file in the given project.
     *
     * @param sourceFile The virtual file representing the source file to insert the method into.
     * @param project The project in which the source file belongs.
     * @param code The code of the method to be inserted.
     * @return `true` if the method was successfully inserted, `false` otherwise.
     */
    fun insertMethod(sourceFile: VirtualFile, project: Project, code: String): Boolean
    /**
     * Inserts a class into the specified source file in the given project.
     *
     * @param sourceFile The virtual file representing the source file to insert the class into.
     * @param project The project in which the source file belongs.
     * @param code The code representing the class to be inserted.
     * @return True if the class was successfully inserted, false otherwise.
     */
    fun insertClass(sourceFile: VirtualFile, project: Project, code: String): Boolean
}