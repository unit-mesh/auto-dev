package cc.unitmesh.devti.parser

import cc.unitmesh.devti.util.parser.CodeFence
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.intellij.lang.annotations.Language

class CodeFenceTest : BasePlatformTestCase() {
    fun testShould_parse_code_from_markdown_java_hello_world() {
        val markdown = """
            |```java
            |public class HelloWorld {
            |    public static void main(String[] args) {
            |        System.out.println("Hello, World");
            |    }
            |}
            |```
        """.trimMargin()

        val code = CodeFence.parse(markdown)

        assertEquals(
            code.text, """
            |public class HelloWorld {
            |    public static void main(String[] args) {
            |        System.out.println("Hello, World");
            |    }
            |}
        """.trimMargin()
        )
        assertTrue(code.isComplete)
    }

    fun testShould_handle_code_not_complete_from_markdown() {
        val markdown = """
            |```java
            |public class HelloWorld {
            |    public static void main(String[] args) {
            |        System.out.println("Hello, World");
        """.trimMargin()

        val code = CodeFence.parse(markdown)
        assertEquals(
            code.text, """
            |public class HelloWorld {
            |    public static void main(String[] args) {
            |        System.out.println("Hello, World");
        """.trimMargin()
        )
        assertTrue(!code.isComplete)
    }

    fun testShould_handle_pure_markdown_content() {
        val content = "```markdown\nGET /wp/v2/posts\n```"
        val code = CodeFence.parse(content)
        assertEquals(code.text, "GET /wp/v2/posts")
    }

    fun testShould_handle_http_request() {
        val content = "```http request\nGET /wp/v2/posts\n```"
        val code = CodeFence.parse(content)
        assertEquals(code.text, "GET /wp/v2/posts")
    }

    fun testShouldParseHtmlCode() {
        val content = """
// patch to call tools for step 3 with DevIns language, should use DevIns code fence
<devin>
/patch:src/main/index.html
```patch
// the index.html code
```
</devin>
""".trimIndent()
        val code = CodeFence.parse(content)
        assertEquals(
            code.text, """
/patch:src/main/index.html
```patch
// the index.html code
```
""".trimIndent()
        )
        assertTrue(code.isComplete)
    }

    fun testShouldParseUndoneHtmlCode() {
        val content = """
// patch to call tools for step 3 with DevIns language, should use DevIns code fence
<devin>
/patch:src/main/index.html
```patch
// the index.html code
```
""".trimIndent()
        val code = CodeFence.parse(content)
        assertFalse(code.isComplete)
        assertEquals(
            code.text, """
/patch:src/main/index.html
```patch
// the index.html code
```
""".trimIndent()
        )
    }

    /// parse all with devins
    fun testShouldParseAllWithDevin() {
        val content = """
            |<devin>
            |// the index.html code
            |</devin>
            |
            |```java
            |public class HelloWorld {
            |    public static void main(String[] args) {
            |        System.out.println("Hello, World");
            |    }
            |}
            |```
        """.trimMargin()

        val codeFences = CodeFence.parseAll(content)
        assertEquals(codeFences.size, 2)
        assertEquals(
            codeFences[0].text, """
            |// the index.html code
        """.trimMargin()
        )
        assertTrue(codeFences[0].isComplete)
        assertEquals(
            codeFences[1].text, """
            |public class HelloWorld {
            |    public static void main(String[] args) {
            |        System.out.println("Hello, World");
            |    }
            |}
        """.trimMargin()
        )
        assertTrue(codeFences[1].isComplete)
    }

    // parse for error devin block, like ```devin\n```java\n
    fun testShouldParseErrorDevinBlock() {
        val content = """
            |```devin
            |/write:HelloWorld.java
            |```java
            |public class HelloWorld {
            |    public static void main(String[] args) {
            |        System.out.println("Hello, World");
            |    }
            |}
            |```
            |
        """.trimMargin()

        val codeFences = CodeFence.parseAll(content)
        assertEquals(codeFences.size, 1)
        assertEquals(
            codeFences[0].text, """
            |/write:HelloWorld.java
            |```java
            |public class HelloWorld {
            |    public static void main(String[] args) {
            |        System.out.println("Hello, World");
            |    }
            |}
            |```
        """.trimMargin()
        )
        assertTrue(codeFences[0].isComplete)
    }

    // parse for error devin block, like ```devin\n```java\n
    fun testShouldParseErrorDevinBlockWithFull() {
        val content = """
            |```devin
            |/write:HelloWorld.java
            |```java
            |public class HelloWorld {
            |    public static void main(String[] args) {
            |        System.out.println("Hello, World");
            |    }
            |}
            |```
            |```
            |
        """.trimMargin()

        val codeFences = CodeFence.parseAll(content)
        assertEquals(codeFences.size, 1)
        assertEquals(
            codeFences[0].text, """
            |/write:HelloWorld.java
            |```java
            |public class HelloWorld {
            |    public static void main(String[] args) {
            |        System.out.println("Hello, World");
            |    }
            |}
            |```
        """.trimMargin()
        )
        assertTrue(codeFences[0].isComplete)
    }

    fun testShouldParseRealWorldCase() {
        val content = """
### 第一步：安装 `vue-router`
```bash
npm install vue-router@4
```

### 第二步：创建路由配置
```devin
/write:src/router.js
```javascript
import { createRouter, createWebHistory } from 'vue-router';
import BlogList from '@/components/BlogList.vue';

const routes = [
  {
    path: '/blog',
    name: 'BlogList',
    component: BlogList,
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

export default router;
```

### 第三步：创建博客列表组件
```devin
/write:src/components/BlogList.vue
```vue
<template>
  <div class="blog-list">
    <h1>博客列表</h1>
    <div class="blog-item" v-for="blog in blogs" :key="blog.id">
      <h2>{{ blog.title }}</h2>
      <p>{{ blog.summary }}</p>
      <small>发布日期: {{ blog.date }}</small>
    </div>
  </div>
</template>

<script>
export default {
  data() {
    return {
      blogs: [],
    };
  },
};
</script>
```

### 第四步：更新 `main.js` 以使用路由

```devin
/patch:src/main.js
```javascript
import { createApp } from 'vue';
import App from './App.vue';
import router from './router';

const app = createApp(App);
app.use(router);
app.mount('#app');
```

### 第五步：更新 `App.vue` 以支持路由

```devin
/patch:src/App.vue
```vue
<template>
  <div id="app">
    <router-view />
  </div>
</template>
```

### 第六步：运行项目
```bash
npm run dev
```

### 完成后的效果
打开浏览器并访问 `http://localhost:3000/blog`，您将看到一个简单的博客列表页，显示两篇示例博客。
"""

        val codeFences = CodeFence.parseAll(content)
        assertEquals(codeFences.size, 13)
        assertEquals(
            codeFences[9].text, """/patch:src/App.vue
```vue
<template>
  <div id="app">
    <router-view />
  </div>
</template>
```""".trimMargin()
        )
        assertEquals(codeFences[11].text, """npm run dev""".trimMargin())
        assertTrue(codeFences[0].isComplete)
    }

    fun testShouldFixForNormalDevinLanguage() {
        val content = """
```devin
/run:src/test/java/cc/unitmesh/untitled/demo/service/BlogServiceTest.java
```

如果测试通过，您可以启动应用程序进行手动测试：

```bash
./gradlew :bootRun
```
        """.trimMargin()

        val codeFences = CodeFence.parseAll(content)
        assertEquals(codeFences.size, 3)
        assertEquals(
            codeFences[0].text, """
            |/run:src/test/java/cc/unitmesh/untitled/demo/service/BlogServiceTest.java
        """.trimMargin()
        )
        assertTrue(codeFences[0].isComplete)
    }

    fun testShouldHandleForContinueDevinBlock() {
        val content = """

现在，我将生成相应的代码变更：

<devin>
/write:src/main/java/cc/unitmesh/untitled/demo/application/BlogApplicationService.java
```java
package cc.unitmesh.untitled.demo.application;

import cc.unitmesh.untitled.demo.entity.BlogPost;
import cc.unitmesh.untitled.demo.repository.BlogRepository;
import cc.unitmesh.untitled.demo.service.BlogDomainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BlogApplicationService {
    @Autowired
    private BlogRepository blogRepository;

    @Autowired
    private BlogDomainService blogDomainService;

    public BlogPost createBlog(BlogPost blogDto) {
        blogDomainService.validateBlog(blogDto);
        return blogRepository.save(blogDto);
    }

    public BlogPost getBlogById(Long id) {
        return blogRepository.findById(id).orElse(null);
    }

    public BlogPost updateBlog(Long id, BlogPost blogDto) {
        return blogRepository.findById(id).map(blog -> {
            blog.setTitle(blogDto.getTitle());
            blog.setContent(blogDto.getContent());
            blogDomainService.validateBlog(blog);
            return blogRepository.save(blog);
        }).orElse(null);
    }

    public void deleteBlog(Long id) {
        blogRepository.deleteById(id);
    }
}
```
</devin>

<devin>
/write:src/main/java/cc/unitmesh/untitled/demo/service/BlogDomainService.java
```java
package cc.unitmesh.untitled.demo.service;

import cc.unitmesh.untitled.demo.entity.BlogPost;
import org.springframework.stereotype.Service;

@Service
public class BlogDomainService {
    public void validateBlog(BlogPost blog) {
        if (blog.getTitle() == null || blog.getTitle().isEmpty()) {
            throw new IllegalArgumentException("Blog title cannot be empty");
        }
        if (blog.getContent() == null || blog.getContent().isEmpty()) {
            throw new IllegalArgumentException("Blog content cannot be empty");
        }
    }
}
```
</devin>

<devin>
/write:src/main/java/cc/unitmesh/untitled/demo/service/BlogDomainService.java
        """.trimMargin()

        val codeFences = CodeFence.parseAll(content)
        assertEquals(codeFences.size, 4)
    }

    fun testShouldHandleCodeUnderListWithErrorIndent() {
        val content = """
1. **通过代码设置属性**：
   你可以在你的插件代码或项目中的某个地方直接使用这段代码来设置属性。例如：

   ```java
   PropertiesComponent.getInstance().setValue("matterhorn.junie.untestedIdeAccepted", "true");
   ```

   这会将 `matterhorn.junie.untestedIdeAccepted` 的值设置为 `"true"`。

2. **通过 IDEA 的配置文件手动设置**：
   如果你不想通过代码来设置这个属性，你可以手动编辑 IDEA 的配置文件。IDEA 的全局属性通常存储在 `idea.properties` 文件中，或者在某些情况下存储在 `options` 目录下的 XML 文件中。
```
        """.trimMargin()

        val codeFences = CodeFence.parseAll(content)
        assertEquals(codeFences.size, 3)
        assertEquals(
            codeFences[0].text, """
            |1. **通过代码设置属性**：
            |   你可以在你的插件代码或项目中的某个地方直接使用这段代码来设置属性。例如：
        """.trimMargin()
        )
        assertTrue(codeFences[0].isComplete)

        assertEquals(
            codeFences[1].text, """
            |PropertiesComponent.getInstance().setValue("matterhorn.junie.untestedIdeAccepted", "true");
        """.trimMargin()
        )
        assertEquals(codeFences[1].originLanguage, "java")

        assertEquals(
            codeFences[2].text, """
            |这会将 `matterhorn.junie.untestedIdeAccepted` 的值设置为 `"true"`。
            |
            |2. **通过 IDEA 的配置文件手动设置**：
            |   如果你不想通过代码来设置这个属性，你可以手动编辑 IDEA 的配置文件。IDEA 的全局属性通常存储在 `idea.properties` 文件中，或者在某些情况下存储在 `options` 目录下的 XML 文件中。
        """.trimMargin()
        )
    }

    fun testShouldIdentifyForMultiplePatch() {
        @Language("DevIn")
        val content = """
<devin>            
```patch
Index: src/main/java/cc/unitmesh/untitled/demo/controller/BlogController.java
--- src/main/java/cc/unitmesh/untitled/demo/controller/BlogController.java
+++ src/main/java/cc/unitmesh/untitled/demo/interfaces/rest/BlogController.java
@@ -3,39 +3,40 @@
 import cc.unitmesh.untitled.demo.dto.CreateBlogRequest;
 import cc.unitmesh.untitled.demo.dto.CreateBlogResponse;
-import cc.unitmesh.untitled.demo.entity.BlogPost;
-import cc.unitmesh.untitled.demo.service.BlogService;
+import cc.unitmesh.untitled.demo.application.BlogCommandService;
+import cc.unitmesh.untitled.demo.application.BlogQueryService;
+import cc.unitmesh.untitled.demo.domain.model.Blog;
+import cc.unitmesh.untitled.demo.domain.model.BlogAssembler;
 import io.swagger.annotations.ApiOperation;
-import org.springframework.beans.BeanUtils;
+import org.springframework.hateoas.EntityModel;
 import org.springframework.web.bind.annotation.*;
 
-import java.util.List;
-
 @RestController
 @RequestMapping("/blog")
 public class BlogController {
-    BlogService blogService;
+    private final BlogCommandService blogCommandService;
+    private final BlogQueryService blogQueryService;
+    private final BlogAssembler blogAssembler;
 
-    public BlogController(BlogService blogService) {
-        this.blogService = blogService;
+    public BlogController(BlogCommandService blogCommandService, 
+                          BlogQueryService blogQueryService,
+                          BlogAssembler blogAssembler) {
+        this.blogCommandService = blogCommandService;
+        this.blogQueryService = blogQueryService;
+        this.blogAssembler = blogAssembler;
     }
 
     @ApiOperation(value = "Get Blog by id")
     @GetMapping("/{id}")
-    public BlogPost getBlog(@PathVariable Long id) {
-        return blogService.getBlogById(id);
+    public EntityModel<BlogResponse> getBlog(@PathVariable Long id) {
+        Blog blog = blogQueryService.getBlog(id);
+        return blogAssembler.toModel(blog);
     }
 
     @ApiOperation(value = "Create a new blog")
     @PostMapping("/")
-    public BlogPost createBlog(@RequestBody CreateBlogRequest request) {
-        CreateBlogResponse response = new CreateBlogResponse();
-        BlogPost blogPost = new BlogPost();
-        BeanUtils.copyProperties(request, blogPost);
-        BlogPost createdBlog = blogService.createBlog(blogPost);
-        BeanUtils.copyProperties(createdBlog, response);
-        return createdBlog;
+    public Long createBlog(@RequestBody CreateBlogRequest request) {
+        Blog blog = blogAssembler.toDomain(request);
+        return blogCommandService.createBlog(blog);
     }
 }
```

```patch
Index: src/main/java/cc/unitmesh/untitled/demo/repository/BlogRepository.java
--- src/main/java/cc/unitmesh/untitled/demo/repository/BlogRepository.java
+++ src/main/java/cc/unitmesh/untitled/demo/infrastructure/persistence/JpaBlogRepository.java
@@ -1,10 +1,17 @@
-package cc.unitmesh.untitled.demo.repository;
+package cc.unitmesh.untitled.demo.infrastructure.persistence;
 
-import cc.unitmesh.untitled.demo.entity.BlogPost;
+import cc.unitmesh.untitled.demo.domain.model.Blog;
+import cc.unitmesh.untitled.demo.domain.repository.BlogRepository;
 import org.springframework.data.repository.CrudRepository;
 import org.springframework.stereotype.Repository;
 
 @Repository
-public interface BlogRepository extends CrudRepository<BlogPost, Long> {
+public interface JpaBlogRepository extends CrudRepository<BlogJpaEntity, Long> {
+}
+
+@Repository
+public class BlogRepositoryImpl implements BlogRepository {
+    private final JpaBlogRepository jpaBlogRepository;
 
+    // 实现领域仓库接口，处理领域对象与JPA实体的转换
 }
```
</devin> 
""".trimIndent()

        val codeFences = CodeFence.parseAll(content)
        assertEquals(codeFences.size, 1)
    }

    fun testShouldParseAllWithXmlTags() {
        val content = """
            首先，我需要思考重构的步骤：
            
            ```plan
            1. 领域模型重构：
            - 将BlogPost实体合并到Blog聚合根，建立完整的领域对象
            - 添加领域行为方法（发布、审核、评论等）
            
            2. 分层结构调整：
            - 清理entity层冗余对象
            ```
            
            然后，我们可以开始代码实现：
            
            ```java
            public class Blog {
                private BlogId id;
                private String title;
                private String content;
                
                public void publish() {
                    // 实现发布逻辑
                }
            }
            ```
            
            <devin>
            /patch
            ```patch
        """.trimIndent()

        val codeFences = CodeFence.parseAll(content)
        assertEquals(5, codeFences.size)
        
        assertEquals("首先，我需要思考重构的步骤：", codeFences[0].text)
        
        assertEquals(
            """
            1. 领域模型重构：
            - 将BlogPost实体合并到Blog聚合根，建立完整的领域对象
            - 添加领域行为方法（发布、审核、评论等）
            
            2. 分层结构调整：
            - 清理entity层冗余对象
            """.trimIndent(), 
            codeFences[1].text
        )
        assertEquals("plan", codeFences[1].extension)
        
        assertEquals("然后，我们可以开始代码实现：", codeFences[2].text)
        assertEquals(
            """
            public class Blog {
                private BlogId id;
                private String title;
                private String content;
                
                public void publish() {
                    // 实现发布逻辑
                }
            }""".trimIndent(),
            codeFences[3].text
        )
        assertEquals("java", codeFences[3].extension)

        assertEquals("DevIn", codeFences[4].originLanguage)
    }

    fun testShouldResolveContinueDevinBlock() {
        val content = """
            我将更新 `CreateBlogRequest` 和 `CreateBlogResponse` 以匹配新的领域模型。

            <devin>
            /patch:src/main/java/cc/unitmesh/untitled/demo/dto/CreateBlogRequest.java
            ```patch
            Index: src/main/java/cc/unitmesh/untitled/demo/dto/CreateBlogRequest.java
            --- src/main/java/cc/unitmesh/untitled/demo/dto/CreateBlogRequest.java
            +++ src/main/java/cc/unitmesh/untitled/demo/dto/CreateBlogRequest.java
            @@ -1,7 +1,7 @@
             package cc.unitmesh.untitled.demo.dto;

            -import cc.unitmesh.untitled.demo.entity.Author;
             import lombok.Data;
            +import cc.unitmesh.untitled.demo.domain.Author;

             @Data
             public class CreateBlogRequest {
            ```
            </devin>

            <devin>
        """.trimIndent()

        val codeFences = CodeFence.parseAll(content)
        assertEquals(3, codeFences.size)
    }

    fun testShouldParseMultipleConsecutiveDevinBlocks() {
        val content = """
            这里有多个连续的 devin 块：

            <devin>
            /write:file1.java
            public class File1 {}
            </devin>

            <devin>
            /write:file2.java
            public class File2 {}
            </devin>

            <devin>
            /write:file3.java
            public class File3 {}
            </devin>

            结束文本
        """.trimIndent()

        val codeFences = CodeFence.parseAll(content)
        assertEquals(5, codeFences.size)

        // 第一个应该是文本块
        assertEquals("这里有多个连续的 devin 块：", codeFences[0].text)
        assertEquals(null, codeFences[0].originLanguage)

        // 第二个应该是第一个 devin 块
        assertEquals("/write:file1.java\npublic class File1 {}", codeFences[1].text)
        assertEquals("DevIn", codeFences[1].originLanguage)
        assertTrue(codeFences[1].isComplete)

        // 第三个应该是第二个 devin 块
        assertEquals("/write:file2.java\npublic class File2 {}", codeFences[2].text)
        assertEquals("DevIn", codeFences[2].originLanguage)
        assertTrue(codeFences[2].isComplete)

        // 第四个应该是第三个 devin 块
        assertEquals("/write:file3.java\npublic class File3 {}", codeFences[3].text)
        assertEquals("DevIn", codeFences[3].originLanguage)
        assertTrue(codeFences[3].isComplete)

        // 第五个应该是结束文本
        assertEquals("结束文本", codeFences[4].text)
        assertEquals(null, codeFences[4].originLanguage)
    }

    fun testShouldResolveContinueDevinBlock2() {
        val content = """
            <devin>
            /write:src/main/java/cc/unitmesh/untitled/demo/domain/valueobjects/BlogPost.java
            ```java
            package cc.unitmesh.untitled.demo.domain.valueobjects;
            
            public class BlogPost {
                private String title;
                private String content;
            
                public BlogPost(String title, String content) {
                    this.title = title;
                    this.content = content;
                }
            
                // Getters and setters
            }
            ```
            </devin>
            
            <devin>
            /write:src/main/java/cc/unitmesh/untitled/demo/domain/valueobjects/Author.java
            ```java
            package cc.unitmesh.untitled.demo.domain.valueobjects;
            
            public class Author {
                private String name;
            
                public Author(String name) {
                    this.name = name;
                }
            
                // Getters and setters
            }
            ```
            </devin>
            
            <devin>
        """.trimIndent()

        val codeFences = CodeFence.parseAll(content)
        assertEquals(3, codeFences.size)
    }
}
