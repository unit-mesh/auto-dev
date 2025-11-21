package cc.unitmesh.agent.vcs.context

fun main() {
    val patch = """
        diff --git a/src/User.kt b/src/User.kt
        index abc1234..def5678 100644
        --- a/src/User.kt
        +++ b/src/User.kt
        @@ -13,7 +13,10 @@ class UserService {
             fun processUser(user: User?) {
        -        println(user.name)
        +        if (user == null) {
        +            throw IllegalArgumentException("User cannot be null")
        +        }
        +        println(user.name)
             }
         }
    """.trimIndent()

    val extractor = ChangedCodeExtractor()
    val result = extractor.extractChangedHunks(patch)

    println("Files found: ${result.keys}")
    result.forEach { (file, hunks) ->
        println("\nFile: $file")
        println("Hunks: ${hunks.size}")
        hunks.forEachIndexed { index, hunk ->
            println("\n  Hunk #$index:")
            println("    Old start: ${hunk.oldStartLine}, New start: ${hunk.newStartLine}")
            println("    Context before (${hunk.contextBefore.size}): ${hunk.contextBefore}")
            println("    Deleted (${hunk.deletedLines.size}): ${hunk.deletedLines}")
            println("    Added (${hunk.addedLines.size}): ${hunk.addedLines}")
            println("    Context after (${hunk.contextAfter.size}): ${hunk.contextAfter}")
        }
    }
}
