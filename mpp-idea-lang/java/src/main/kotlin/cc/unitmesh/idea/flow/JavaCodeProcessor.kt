package cc.unitmesh.idea.flow

import com.intellij.openapi.application.runReadAction
import com.intellij.psi.PsiJavaFile

class JavaCodeProcessor {
    companion object {
        /**
         * @param code: code in string
         * @param calleeName: the usage name of the service, like: "blogService"
         */
        fun findUsageCode(code: String, calleeName: String): List<String> {
            // a serviceName should be uppercase, we need to convert first letter to lowercase
            val usedServiceName = calleeName.replaceFirstChar { it.lowercase() }

            //  use regex to match " xxxServer.xxxMethod(xxx);
            val usedMethodCode = Regex("""\s+($usedServiceName\..*\(.*\))[^\n]""")
                .findAll(code)
                .map { it.value.trim() }
                .toList()

            return usedMethodCode
        }

        /**
         * @param serviceFile: the [PsiJavaFile] file
         * @param usedMethod: the used method in controller, like: ["blogService.getAllBlogPosts()", "blogService.createBlog(blogPost);"]
         * @return: the method name that not exist in service, like: ["deleteBlogPost"]
         */
        fun findNoExistMethod(serviceFile: PsiJavaFile, usedMethod: List<String>): List<String> {
            // regex to match "getAllBlogPosts" from "blogService.getAllBlogPosts();"
            val allUsedMethod =
                usedMethod.mapNotNull {
                    Regex(""".*\.(\w+)\(.*\)""")
                        .find(it)
                        ?.groupValues
                        ?.get(1)
                }

            return runReadAction {
                val serviceClass = serviceFile.classes[0]
                val serviceMethod = serviceClass.methods
                val serviceMethodNames = serviceMethod.map { it.name }

                // if allUsedMethod is not in serviceMethodNames, then it is not exist
                return@runReadAction allUsedMethod.filter { !serviceMethodNames.contains(it) }
            }
        }
    }
}