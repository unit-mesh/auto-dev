## 要求

Document: https://plantuml.com/zh/class-diagram

| **字符** | **图标(属性)**                                              | **图标(方法)**                                               | **可访问性**               |
|--------|---------------------------------------------------------|----------------------------------------------------------|------------------------|
| `-`    | ![](https://plantuml.com/img/private-field.png)         | ![](https://plantuml.com/img/private-method.png)         | `private` 私有           |
| `#`    | ![](https://plantuml.com/img/protected-field.png)       | ![](https://plantuml.com/img/protected-method.png)       | `protected` 受保护        |
| `~`    | ![](https://plantuml.com/img/package-private-field.png) | ![](https://plantuml.com/img/package-private-method.png) | `package private` 包内可见 |
| `+`    | ![](https://plantuml.com/img/public-field.png)          | ![](https://plantuml.com/img/public-method.png)          | `public` 公有            |

Strategy:

1. prefix, suffix
2. no imports
3. UML: service, dto, entity
4. exceptions
5. no getter/setter
6. if is a class, then no `;` at the end

## Prompt

Complete java code, return rest code, no explaining.

```java
// compare following uml
// package: cc.unitmesh.untitled.demo.service
//class BlogService {
//  blogRepository: BlogRepository
//  + createBlog(blogDto: CreateBlogDto): BlogPost
//  + getAllBlogPosts(): List<BlogPost>
//  ' some other methods
//}
//
// package: cc.unitmesh.untitled.demo.dto
// getter/setter: title: String, content: String, author: String
//class CreateBlogDto {
//}
//  
// package: cc.unitmesh.untitled.demo.model
// getter/setter: id: Long, title: String, content: String, author: String  
//class BlogPost {
//  + constructor(title: String, content: String, author: String)
//}
package cc.unitmesh.untitled.demo.controller;

import cc.unitmesh.untitled.demo.dto.CreateBlogDto;
import cc.unitmesh.untitled.demo.entity.BlogPost;
import cc.unitmesh.untitled.demo.service.BlogService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Controller
public class BlogController {
BlogService blogService;

    public BlogController(BlogService blogService) {
        this.blogService = blogService;
    }
    
    // return all blog posts
```
