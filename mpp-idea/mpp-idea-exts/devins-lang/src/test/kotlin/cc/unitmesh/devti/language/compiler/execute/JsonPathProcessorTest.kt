package cc.unitmesh.devti.language.compiler.execute

import cc.unitmesh.devti.language.ast.action.PatternActionFunc
import cc.unitmesh.devti.language.processor.JsonPathProcessor
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.Test

class JsonPathProcessorTest : BasePlatformTestCase() {
    fun testShouldParseJsonStringWithValidJsonPath() {
        // given
        val jsonStr = "{\"key\":\"value\"}"
        val action = PatternActionFunc.JsonPath(null, "key")

        // when
        val result = JsonPathProcessor.execute(project, jsonStr, action)

        // then
        assertEquals("value", result)
    }

    fun testShouldReturnNullWhenJsonPathDoesNotExistInJsonString() {
        // given
        val jsonStr = "{\"key\":\"value\"}"
        val action = PatternActionFunc.JsonPath(null, "invalidKey")

        // when
        val result = JsonPathProcessor.execute(project, jsonStr, action)

        // then
        assertNull(result)
    }

    fun testShouldParseSseResultWithValidJsonPath() {
        // given
        val sseInput =
            "data: {\"event\":\"agent_message\",\"conversation_id\":\"48929266-a58f-46cc-a5eb-33145e6a96ef\",\"message_id\":\"91ad550b-1109-4062-88f8-07be18238e0e\",\"created_at\":1725437154,\"task_id\":\"4f846104-8571-42f1-b04c-f6f034b2fe9e\",\"id\":\"91ad550b-1109-4062-88f8-07be18238e0e\",\"answer\":\"The\"}\n"
        val jsonPath = "answer"

        // when
        val result = JsonPathProcessor.parseSSEResult(sseInput, jsonPath)

        // then
        assertEquals("The", result)
    }

    fun testShouldReturnEmptyStringWhenJsonPathDoesNotExistInSseResult() {
        // given
        val sseInput =
            "data: {\"event\":\"agent_message\",\"conversation_id\":\"48929266-a58f-46cc-a5eb-33145e6a96ef\",\"message_id\":\"91ad550b-1109-4062-88f8-07be18238e0e\",\"created_at\":1725437154,\"task_id\":\"4f846104-8571-42f1-b04c-f6f034b2fe9e\",\"id\":\"91ad550b-1109-4062-88f8-07be18238e0e\",\"answer\":\"The\"}\n"
        val jsonPath = "invalidKey"

        // when
        val result = JsonPathProcessor.parseSSEResult(sseInput, jsonPath)

        // then
        assertEquals("", result)
    }

    fun testShouldParseMultipleSseDataLinesWithValidJsonPath() {
        // given
        val sseInput =
            "data: {\"event\":\"agent_message\",\"conversation_id\":\"48929266-a58f-46cc-a5eb-33145e6a96ef\",\"message_id\":\"91ad550b-1109-4062-88f8-07be18238e0e\",\"created_at\":1725437154,\"task_id\":\"4f846104-8571-42f1-b04c-f6f034b2fe9e\",\"id\":\"91ad550b-1109-4062-88f8-07be18238e0e\",\"answer\":\"The\"}\n" +
                    "data: {\"event\":\"message_end\",\"conversation_id\":\"48929266-a58f-46cc-a5eb-33145e6a96ef\",\"message_id\":\"91ad550b-1109-4062-88f8-07be18238e0e\",\"created_at\":1725437154,\"task_id\":\"4f846104-8571-42f1-b04c-f6f034b2fe9e\",\"id\":\"91ad550b-1109-4062-88f8-07be18238e0e\"}\n"
        val jsonPath = "$.event"

        // when
        val result = JsonPathProcessor.parseSSEResult(sseInput, jsonPath)

        // then
        assertEquals("agent_messagemessage_end", result)
    }
}
