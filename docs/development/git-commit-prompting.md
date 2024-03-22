---
layout: default
title: Git Commit
nav_order: 10
parent: Development
---

Git commit Prompt example

    Write a cohesive yet descriptive commit message for a given diff.
    Make sure to include both information What was changed and Why.
    Start with a short sentence in imperative form, no more than 50 characters long.
    Then leave an empty line and continue with a more detailed explanation, if necessary.
    Explanation should have less than 200 characters.
    
    examples:
    - fix(authentication): add password regex pattern
    - feat(storage): add new test cases
    - test(java): fix test case for user controller
    
    Diff:
    
    ```diff
    modify file src/main/kotlin/cc/unitmesh/devti/prompting/VcsPrompting.kt
         private val defaultIgnoreFilePatterns: List<PathMatcher> = listOf(
             "**/*.md", "**/*.json", "**/*.jsonl", "**/*.txt", "**/*.xml", "**/*.yml", "**/*.yaml", "**/*.html",
             "**/*.log", "**/*.tmp", "**/*.temp", "**/*.bak", "**/*.swp",
    -         "**/*.svg",
    +        "**/*.svg",
         ).map {
             FileSystems.getDefault().getPathMatcher("glob:$it")
         }
             project: Project,
             ignoreFilePatterns: List<PathMatcher> = defaultIgnoreFilePatterns,
         ): String? {
    +        val changeText = project.service<DiffSimplifier>().simplify(selectList, ignoreFilePatterns)
    +
    +        if (changeText.isEmpty()) {
    +            return null
    +        }
    +
    +        val processedText = DiffSimplifier.postProcess(changeText)
             val writer = StringWriter()
             if (details.isNotEmpty()) {
                 details.forEach { writer.write(it.fullMessage + "\n\n") }
             }
    -        writer.write("Changes:\n\n")
    -        val changeText = project.service<DiffSimplifier>().simplify(selectList, ignoreFilePatterns)
    -
    -        if (changeText.isEmpty()) {
    -            return null
    -        }
    +        writer.write(
    +            """
    +            Changes:
    +            
    +            ```patch
    +            $processedText
    +            ```
    +            """.trimIndent()
    +        )
    -
    -
    -        writer.write("```patch\n\n")
    -        writer.write(DiffSimplifier.postProcess(changeText))
    -        writer.write("\n\n```\n\n")
    -
             return writer.toString()
         }
         fun hasChanges(): List<Change> {
    -        val changeListManager = ChangeListManagerImpl.getInstance(project)
    -        val changes = changeListManager.changeLists.flatMap {
    -            it.changes
    -        }
    -
    -        return changes
    +        val changeListManager = ChangeListManager.getInstance(project)
    +        return changeListManager.changeLists.flatMap { it.changes }
         }
     }
    ```
    
    