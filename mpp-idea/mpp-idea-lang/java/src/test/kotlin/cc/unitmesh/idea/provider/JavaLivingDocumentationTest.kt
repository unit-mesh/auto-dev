package cc.unitmesh.idea.provider

import com.intellij.testFramework.LightPlatformTestCase
import org.junit.Test

class JavaLivingDocumentationTest {
    @Test
    fun should_handle_when_llm_return_error_java_code() {
        val code = """```Java
/**
    Converts a byte array to a short array.
    @param bytes the input byte array.
    @return the converted short array. If the input byte array is null, returns null.
    */
    public short[] bytesToShort(byte[] bytes) {
    if (bytes == null) {
    return null;
    }
    short[] shorts = new short[bytes.length / 2];
    ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);
    return shorts;
    }
"""

        val result = JavaLivingDocumentation.preHandleDoc(code)
        LightPlatformTestCase.assertEquals(result, """/**
    Converts a byte array to a short array.
    @param bytes the input byte array.
    @return the converted short array. If the input byte array is null, returns null.
    */""")
    }

    @Test
    fun should_handle_normal_java_code() {
        val code = """/**
    Converts a byte array to a short array.
    @param bytes the input byte array.
    @return the converted short array. If the input byte array is null, returns null.
    */"""

        val result = JavaLivingDocumentation.preHandleDoc(code)
        LightPlatformTestCase.assertEquals(result, """/**
    Converts a byte array to a short array.
    @param bytes the input byte array.
    @return the converted short array. If the input byte array is null, returns null.
    */""")
    }
}
