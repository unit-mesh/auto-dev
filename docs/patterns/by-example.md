---
layout: default
title: By Example
nav_order: 1
parent: Design Patterns
---

> Finding example content as context to generate as input for the generative AI, based on current user habits.

## Git Commit message Example

Implementation steps:

1. Retrieve version control system (VCS) log providers for the current project.
2. Get the current branch and user.
3. Filter logs based on user or branch.
4. Collect example submission information.

### Code Implementation

```kotlin
private fun findExampleCommitMessages(project: Project): String? {
    val logProviders = VcsProjectLog.getLogProviders(project)
    logProviders.entries.firstOrNull() ?: return null

    val logProvider = entry.value
    val branch = logProvider.getCurrentBranch(entry.key) ?: return null
    val user = logProvider.getCurrentUser(entry.key)

    val logFilter = if (user != null) {
        VcsLogFilterObject.collection(VcsLogFilterObject.fromUser(user, setOf()))
    } else {
        VcsLogFilterObject.collection(VcsLogFilterObject.fromBranch(branch))
    }

    return collectExamples(logProvider, entry.key, logFilter)
}
```

### Template Example
       
    Write a cohesive yet descriptive commit message for a given diff.
    - Make sure to include both information What was changed and Why.
    - Start with a short sentence in imperative form, no more than 50 characters long.
    - Then leave an empty line and continue with a more detailed explanation, if necessary.
    - Explanation should have less than 200 characters.
    
    Follow the Conventional Commits specification, examples:
    - fix(authentication): fix password regex pattern case
    - feat(storage): add support for S3 storage
    - test(java): fix test case for user controller
    - docs(docs): add architecture diagram to home page
    
    #if( $context.historyExamples.length() > 0 )
    Here is History Examples:
    $context.historyExamples
    #end
    
    Diff:
    
    ```diff
    ${context.diffContent}

## Code Example: Testing Code Samples

Reference language implementations: `JavaTestContextProvider`, `KotlinTestContextProvider`.

Steps:

1. Retrieve the tested code for the current project.
2. Find templates based on the tested code: `Controller`, `Service`, default tests, etc.
   - Kotlin: `ControllerTest.kt`, `ServiceTest.kt`, `Test.kt`
   - Java: `ControllerTest.java`, `ServiceTest.java`, `Test.java`
3. Generate testing code based on templates.

## Document Example

DOC TODO
```