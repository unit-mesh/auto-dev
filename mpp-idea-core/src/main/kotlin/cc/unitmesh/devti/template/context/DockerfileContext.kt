package cc.unitmesh.devti.template.context

class DockerfileContext(
    val buildToolName: String,
    val buildToolVersion: String,
    val languageName: String,
    val languageVersion: String,
    /**
     * The `taskString` variable represents a string that can be used to store various types of task information or commands.
     *
     * This variable can be used in different scenarios, such as:
     * - Parsing the `package.json` file and retrieving the scripts field.
     * - Running a Gradle task to obtain a list of all available tasks.
     * - Storing any other task-related information or commands.
     *
     * The content of the `taskString` variable will depend on the specific use case and can be customized accordingly.
     *
     * Example usage:
     * ```
     * val taskString: String = "npm run build"
     * ```
     * In this example, the `taskString` variable is assigned the value `"npm run build"`, which represents a command to run the `build` script defined in the `package.json` file.
     *
     * Please note that the actual content and usage of the `taskString` variable will vary depending on the context and requirements of the application or system using it.
     */
    val taskString: String = "",
) : TemplateContext
