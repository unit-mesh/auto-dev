package cc.unitmesh.devti.flow

import org.junit.Test

class JavaParseUtilTest {
    val demoCode = """
// Request DTO
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
}

// Response DTO
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
}

// Entity
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
}
    """.trimIndent()

    @Test
    fun splitClass() {
        val classes = JavaParseUtil.splitClass(demoCode)
        assert(classes.size == 3)
        assert(classes[0].contains("CreateBlogRequest"))
        assert(classes[1].contains("CreateBlogResponse"))
        assert(classes[2].contains("BlogPost"))
    }
}