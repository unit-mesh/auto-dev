package cc.unitmesh.devti.llms.custom

import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.Option
import io.kotest.matchers.shouldBe
import org.junit.Test

class CustomLLMProviderTest {
    @Test
    fun should_test_json_path() {
        val content = """{
    "choices": [
        {
            "role": "assistant",
            "content": "It seems like you didn't say anything. Is there anything specific you would like to know or talk about?"
        }
    ],
    "model": "cd-chatglm2-6b_streaming"
}"""
        val conf = Configuration.defaultConfiguration().addOptions(Option.DEFAULT_PATH_LEAF_TO_NULL)
        val document = conf.jsonProvider().parse(content)
        val result = JsonPath.read<String>(document, "$.choices[0].content")

        result shouldBe "It seems like you didn't say anything. Is there anything specific you would like to know or talk about?"
    }
}