---
layout: default
title: Domain Driven Design
nav_order: 2
parent: Scenes
permalink: /scenes/domain-drive-design
---

## Test Prompts

Write unit test for following code.
You MUST return code only, not explain.
You MUST Use English to reply me!
You are working on a project that uses Spring MVC,Spring WebFlux,JDBC to build RESTful APIs.
You MUST use should_xx_xx style for test method name.
You MUST use given-when-then style.
- Test file should be complete and compilable, without need for further actions.
- Ensure that each test focuses on a single use case to maintain clarity and readability.
- Instead of using `@BeforeEach` methods for setup, include all necessary code initialization within each individual test method, do not write parameterized tests.
  | This project uses JUnit 5, you should import `org.junit.jupiter.api.Test` and use `@Test` annotation.- You MUST use MockMvc and test API only.
- Use appropriate Spring test annotations such as `@MockBean`, `@Autowired`, `@WebMvcTest`, `@DataJpaTest`, `@AutoConfigureTestDatabase`, `@AutoConfigureMockMvc`, `@SpringBootTest` etc.


// here are related classes:
// 'filePath: /Users/phodal/IdeaProjects/untitled/src/main/java/cc/unitmesh/untitled/demo/service/BlogService.java
// class BlogService {
//   blogRepository
//   + public BlogPost createBlog(BlogPost blogDto)
//   + public BlogPost getBlogById(Long id)
//   + public BlogPost updateBlog(Long id, BlogPost blogDto)
//   + public void deleteBlog(Long id)
// }
// 'filePath: /Users/phodal/IdeaProjects/untitled/src/main/java/cc/unitmesh/untitled/demo/dto/CreateBlogRequest.java
// class CreateBlogRequest {
//   title
//   content
//   user
//   
// }
// 'filePath: /Users/phodal/IdeaProjects/untitled/src/main/java/cc/unitmesh/untitled/demo/entity/BlogPost.java
// class BlogPost {
//   id
//   title
//   content
//   author
//   + public BlogPost(String title, String content, String author)
//   + public BlogPost()
//   + public void setId(Long id)
//   + public Long getId()
//   + public String getTitle()
//   + public void setTitle(String title)
//   + public String getContent()
//   + public void setContent(String content)
//   + public String getAuthor()
//   + public void setAuthor(String author)
// }
```java
@ApiOperation(value = "Create a new blog")
    @PostMapping("/")
    public BlogPost createBlog(@RequestBody CreateBlogRequest request) {
        CreateBlogResponse response = new CreateBlogResponse();
        BlogPost blogPost = new BlogPost();
        BeanUtils.copyProperties(request, blogPost);
        BlogPost createdBlog = blogService.createBlog(blogPost);
        BeanUtils.copyProperties(createdBlog, response);
        return createdBlog;
    }
```
Start  with `import` syntax here:  
