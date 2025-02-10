package cc.unitmesh.devti.parser

import cc.unitmesh.devti.util.parser.CodeFence
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class CodeFenceTest  : BasePlatformTestCase() {
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
        assertEquals(codeFences.size, 3)
        assertEquals(
            codeFences[0].text, """
            |// the index.html code
        """.trimMargin()
        )
        assertTrue(codeFences[0].isComplete)
        assertEquals(
            codeFences[2].text, """
            |public class HelloWorld {
            |    public static void main(String[] args) {
            |        System.out.println("Hello, World");
            |    }
            |}
        """.trimMargin()
        )
        assertTrue(codeFences[2].isComplete)
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
        assertEquals(codeFences.size, 2)
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

    // real world case
    fun testShouldParseRealWorldCase() {
        val content = """
根据 `package.json` 文件的内容，您的项目目前仅安装了 Vue 3 和 Vite 相关的基本依赖。为了创建博客列表页并实现路由功能，我们需要安装 `vue-router` 作为项目的依赖。

### 第一步：安装 `vue-router`
我们需要使用 `vue-router` 来管理博客列表页的路由。请运行以下命令安装 `vue-router`：
```bash
npm install vue-router@4
```

### 第二步：创建路由配置
接下来，我们需要创建路由配置文件 `router.js` 并定义博客列表页的路由。

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
我们需要创建一个新的组件 `BlogList.vue` 来显示博客列表。

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
      blogs: [
        {
          id: 1,
          title: 'Vue 3 入门指南',
          summary: '本文介绍如何快速上手 Vue 3。',
          date: '2024-01-15',
        },
        {
          id: 2,
          title: 'Vite 使用技巧',
          summary: '分享一些 Vite 的实用技巧。',
          date: '2024-02-01',
        },
      ],
    };
  },
};
</script>

<style scoped>
.blog-list {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
}
.blog-item {
  margin-bottom: 20px;
  padding: 10px;
  border-bottom: 1px solid #ddd;
}
</style>
```

### 第四步：更新 `main.js` 以使用路由
我们需要在 `main.js` 中引入路由器并挂载到 Vue 应用。

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
我们需要在 `App.vue` 中添加路由视图。

```devin
/patch:src/App.vue
```vue
<template>
  <div id="app">
    <router-view />
  </div>
</template>

<script>
export default {
  name: 'App',
};
</script>

<style>
/* 可以根据需要添加全局样式 */
</style>
```

### 第六步：运行项目
一切准备就绪后，您可以通过以下命令启动项目：
```bash
npm run dev
```

### 完成后的效果
打开浏览器并访问 `http://localhost:3000/blog`，您将看到一个简单的博客列表页，显示两篇示例博客。

如果您需要进一步调整样式或功能，请随时告诉我！
        """

        val codeFences = CodeFence.parseAll(content)
        assertEquals(codeFences.size, 13)
        assertEquals(codeFences[9].text, """/patch:src/App.vue
```vue
<template>
  <div id="app">
    <router-view />
  </div>
</template>

<script>
export default {
  name: 'App',
};
</script>

<style>
/* 可以根据需要添加全局样式 */
</style>
```""".trimMargin())
        assertEquals(codeFences[11].text, """npm run dev""".trimMargin())
        assertTrue(codeFences[0].isComplete)
    }

//    fun testShouldFixForNormalDevinLanguage() {
//        val content = """
//```devin
///run:src/test/java/cc/unitmesh/untitled/demo/service/BlogServiceTest.java
//```
//
//如果测试通过，您可以启动应用程序进行手动测试：
//
//```bash
//./gradlew :bootRun
//```
//        """.trimMargin()
//
//        val codeFences = CodeFence.parseAll(content)
//        assertEquals(codeFences.size, 3)
//        assertEquals(
//            codeFences[0].text, """
//            |/run:src/test/java/cc/unitmesh/untitled/demo/service/BlogServiceTest.java
//        """.trimMargin()
//        )
//        assertTrue(codeFences[0].isComplete)
//    }
}
