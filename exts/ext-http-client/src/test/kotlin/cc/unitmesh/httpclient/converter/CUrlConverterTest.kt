package cc.unitmesh.httpclient.converter

import cc.unitmesh.devti.envior.ShireEnvReader
import cc.unitmesh.devti.envior.ShireEnvVariableFiller
import com.intellij.httpClient.converters.curl.parser.CurlParser
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class CUrlConverterTest : BasePlatformTestCase() {
    fun testShouldConvertCurlToRestClientRequest() {
        // Given
        val content = "curl -X POST http://example.com/api/resource -d 'data'"
        val request = CurlParser().parseToRestClientRequest(content)

        // When
        val restClientRequest = CUrlConverter.convert(request = request)

        // Then
        assertEquals("http://example.com/api/resource", restClientRequest.url.toString())
    }

    fun testShouldCallHttpClientWithConvertedRequest() {
        // Given
        val content = "curl --location 'https://open.bigmodel.cn/api/paas/v4/chat/completions' \\\n" +
                "--header 'Authorization: Bearer \$YourKey' \\\n" +
                "--header 'Content-Type: application/json' \\\n" +
                "--data '{\n" +
                "    \"model\": \"glm-4\",\n" +
                "    \"messages\": [\n" +
                "        {\n" +
                "            \"role\": \"user\",\n" +
                "            \"content\": \"你好\"\n" +
                "        }\n" +
                "    ]\n" +
                "}'"
        val req = CurlParser().parseToRestClientRequest(content)
        // When
        val request = CUrlConverter.convert(request = req)

        // Then
        assertEquals("https://open.bigmodel.cn/api/paas/v4/chat/completions", request.url.toString())
    }

    fun testShouldHandleForVariable() {
        val jsonEnv = """
            {
              "development": {
                "name": "Phodal"
              }
            }
             """.trimIndent()

        val envPsiFile = myFixture.addFileToProject("demo.shireEnv.json", jsonEnv)

        val variables = listOf(setOf("development"))
        val obj = ShireEnvReader.getEnvObject("development", envPsiFile)

        // Given
        val messageBody = "Hello \${name}, my name is \${myName}!"

        // When
        val result = ShireEnvVariableFiller.fillVariables(messageBody, variables, obj, mapOf("myName" to "DevIn"))

        // Then
        assertEquals("Hello Phodal, my name is DevIn!", result)
    }
}
