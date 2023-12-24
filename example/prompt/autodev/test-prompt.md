Write unit test for following code. You MUST Use English to reply me!
You are working on a project that uses Spring MVC,Spring WebFlux,JDBC to build RESTful APIs.
You MUST use should_xx_xx style for test method name.
You MUST use given-when-then style.
- Test file should be complete and compilable, without need for further actions.
- Ensure that each test focuses on a single use case to maintain clarity and readability.
- Instead of using `@BeforeEach` methods for setup, include all necessary code initialization within each individual test method, do not write parameterized tests.
  | This project uses JUnit 5, you should import `org.junit.jupiter.api.Test` and use `@Test` annotation.- You MUST use MockMvc and test API only.
- Use appropriate Spring test annotations such as `@MockBean`, `@Autowired`, `@WebMvcTest`, `@DataJpaTest`, `@AutoConfigureTestDatabase`, `@AutoConfigureMockMvc`, `@SpringBootTest` etc.


// here are related classes:
// 'package: cc.unitmesh.untitled.demo.service.BlogService
// class BlogService {
//   blogRepository
//   + public BlogPost createBlog(BlogPost blogDto)
//   + public BlogPost getBlogById(Long id)
//   + public BlogPost updateBlog(Long id, BlogPost blogDto)
//   + public void deleteBlog(Long id)
// }
// 'package: cc.unitmesh.untitled.demo.service.BlogService
// class BlogService {
//   blogRepository
//   + public BlogPost createBlog(BlogPost blogDto)
//   + public BlogPost getBlogById(Long id)
//   + public BlogPost updateBlog(Long id, BlogPost blogDto)
//   + public void deleteBlog(Long id)
// }
// 'package: cc.unitmesh.untitled.demo.entity.BlogPost
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
// 'package: cc.unitmesh.untitled.demo.dto.CreateBlogRequest
// class CreateBlogRequest {
//   title
//   content
//   user
//   
// }
Code:
```java
@RestController
@RequestMapping("/blog")
public class BlogController {
    BlogService blogService;

    public BlogController(BlogService blogService) {
        this.blogService = blogService;
    }

    @ApiOperation(value = "Get Blog by id")
    @GetMapping("/{id}")
    public BlogPost getBlog(@PathVariable Long id) {
        return blogService.getBlogById(id);
    }

    @ApiOperation(value = "Create a new blog")
    @PostMapping("/")
    public BlogPost createBlog(@RequestBody CreateBlogRequest request) {
        BlogPost blogPost = new BlogPost();
        BeanUtils.copyProperties(request, blogPost);
        return blogService.createBlog(blogPost);
    }
}
```
Start  with `import` syntax here:  
