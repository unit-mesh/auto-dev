package cc.unitmesh.idea.java

import cc.unitmesh.idea.flow.JavaParseUtil
import org.junit.Assert.assertEquals
import org.junit.Test

class JavaParseUtilTest {
    private val requestCode = """// Request DTO
public class CreateBlogRequest {
    private String title;
    private String content;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}"""

    private val responseCode = """// Response DTO
public class CreateBlogResponse {
    private long id;
    private String title;
    private String content;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}"""

    private val entityCode = """// Entity
public class BlogPost {
    private long id;
    private String title;
    private String content;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}"""
    private val demoCode = requestCode + responseCode + entityCode

    @Test
    fun splitClass() {
        val classes = JavaParseUtil.splitClass(demoCode)
        assert(classes.size == 3)
        assert(classes[0].contains("CreateBlogRequest"))
        assertEquals(classes[0], requestCode)

        assert(classes[1].contains("CreateBlogResponse"))
        assertEquals(classes[1], responseCode)

        assert(classes[2].contains("BlogPost"))
        assertEquals(classes[2], entityCode)
    }
}