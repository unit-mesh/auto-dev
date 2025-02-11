package cc.unitmesh.devti.parser

import cc.unitmesh.devti.util.parser.CodeFence
import com.intellij.testFramework.fixtures.BasePlatformTestCase

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
}
