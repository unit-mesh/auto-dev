package cc.unitmesh.idea.flow

object JavaParseUtil {
    // split java code string to multiple class string
    // like: public class A {} public class B {} => [public class A {}, public class B {}]
    fun splitClass(code: String): List<String> {
        val classes = mutableListOf<String>()
        var classStart = 0
        var braceCount = 0

        for (i in code.indices) {
            if (code[i] == '{') {
                braceCount++
            } else if (code[i] == '}') {
                braceCount--
            }

            if (braceCount == 0 && code[i] == '}') {
                classes.add(code.substring(classStart, i + 1))
                classStart = i + 1
            }
        }

        return classes
    }
}