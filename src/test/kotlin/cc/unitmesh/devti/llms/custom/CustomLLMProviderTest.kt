package cc.unitmesh.devti.llms.custom

import junit.framework.TestCase.assertEquals
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import okhttp3.Request
import org.junit.Test

class CustomLLMProviderTest {

    @Test
    fun testCustomRequestHeader_addField() {
        val customRequestFormatWithCustomHeader = """
            {
                "customHeaders": 
                    {"header1": "headerV1", "header2": "headerV2"}
            }
        """.trimIndent()
        val builder: Request.Builder = Request.Builder().url("http://localhost")
        builder.appendCustomHeaders(customRequestFormatWithCustomHeader)
        val request = builder.build()

        assertEquals("headerV1", request.header("header1"))
        assertEquals("headerV2", request.header("header2"))
    }

    @Test
    fun testCustomRequestTopLevelBodyField() {
        val customRequest = """
            {
                "customFields": 
                    {"user": "userid", "date": "2012"}
            }
        """.trimIndent()

        val oriObj = buildJsonObject {}
        val jsonObj = oriObj.updateCustomBody(customRequest)

        assertEquals("userid", jsonObj["user"]!!.jsonPrimitive.content)
        assertEquals("2012", jsonObj["date"]!!.jsonPrimitive.content)
    }

    @Test
    fun testCustomRequestMessageKeys() {
        val customRequest = """
            {
                "messageKeys": 
                   {"content": "content"}
            }
        """.trimIndent()
        val request = CustomRequest(listOf(
                Message("user", "this is message"),
        ))
        val oriMessage = Json.parseToJsonElement(Json.encodeToString<CustomRequest>(request)).jsonObject

        val newObj = oriMessage.updateCustomBody(customRequest)


        val messageObj = newObj.jsonObject["messages"]!!.jsonArray
        assertEquals(1, messageObj.size)
        assertEquals("this is message", messageObj[0].jsonObject["content"]!!.jsonPrimitive.content)
    }

    @Test
    fun testCustomRequestUpdate() {
        val customRequestFormat = """
            {
                "customFields": 
                    {"user": "userid", "date": "2012"},
                "messageKeys": 
                   {"content": "anyContentKey", "role": "anyRoleKey"}
            }
        """.trimIndent()

        val customRequest = CustomRequest(listOf(
                Message("robot", "hello")
        ))

        val request = customRequest.updateCustomFormat(customRequestFormat)
        assertEquals("""
            {"messages":[{"anyRoleKey":"robot","anyContentKey":"hello"}],"user":"userid","date":"2012"}
        """.trimIndent(), request)
    }

}
