# Custom Action

You can customize your prompt in `Settings` -> `Tools` -> `AutoDev`

```json
{
  "spec": {
    "controller": "- 在 Controller 中使用 BeanUtils.copyProperties 进行 DTO 转换 Entity\n- 禁止使用 Autowired\n-使用 Swagger Annotation 表明 API 含义\n-Controller 方法应该捕获并处理业务异常，不应该抛出系统异常。",
    "service": "- Service 层应该使用构造函数注入或者 setter 注入，不要使用 @Autowired 注解注入。",
    "entity": "- Entity 类应该使用 JPA 注解进行数据库映射\n- 实体类名应该与对应的数据库表名相同。实体类应该使用注解标记主键和表名，例如：@Id、@GeneratedValue、@Table 等。",
    "repository": "- Repository 接口应该继承 JpaRepository 接口，以获得基本的 CRUD 操作",
    "ddl": "-  字段应该使用 NOT NULL 约束，确保数据的完整性"
  },
  "prompts": [
    {
      "title": "\uD83D\uDE80 \uD83D\uDE80 Code complete",
      "autoInvoke": true,
      "matchRegex": ".*",
      "priority": 1,
      "template": "Code complete:\n${METHOD_INPUT_OUTPUT}\n${SPEC_controller}\n\n${SELECTION}"
    },
    {
      "title": "\uD83C\uDF10\uD83C\uDF10 Translate to Kotlin",
      "autoInvoke": false,
      "matchRegex": ".*",
      "priority": 0,
      "template": "Translate follow code to Kotlin.\n${SIMILAR_CHUNK}\nCompare this snippets: \n${METHOD_INPUT_OUTPUT}\nHere is the code: \n${SELECTION}"
    }
  ]
}
```

- title: the action name
- autoInvoke: auto invoke this action when you perform action
- matchRegex: TODO()
- priority: the priority of the action, the higher will be first. (0~1000 was recommended)
- template: the template of the action, you can use `${SPEC_controller}` to insert spec, `${SELECTION}` to insert
  selected code.

## Variables

Context Variable:

- `${SELECTION}`: the selected code
- `${SIMILAR_CHUNK}`: the similar code chunk
- `${METHOD_INPUT_OUTPUT}`: the method input and output

Spec variables:

- `${SPEC_*}`: load spec from `spec` section in config, like `${SPEC_controller}` will load `spec.controller` from
  config.

### Template Examples

Config:

```json
{
  "title": "\uD83C\uDF10\uD83C\uDF10 Translate to Kotlin",
  "autoInvoke": false,
  "matchRegex": ".*",
  "priority": 0,
  "template": "Translate follow code to Kotlin. Similar chunk: ${SIMILAR_CHUNK} Compare this snippets: ${METHOD_INPUT_OUTPUT}\n \n${SELECTION}"
}
```

Output example:

```
Translate follow code to Kotlin. Similar chunk: // Compare this snippet from java/cc/unitmesh/untitled/demo/controller/CommentController.java:
// public class CommentController {
// Compare this snippet from java/cc/unitmesh/untitled/demo/DemoApplication.java:
// public class DemoApplication {
// Compare this snippet from java/cc/unitmesh/untitled/demo/controller/AdvertiseController.java:
// public class AdvertiseController {
// Compare this snippet from java/cc/unitmesh/untitled/demo/dto/BookMeetingRoomRequest.java:
// public class BookMeetingRoomRequest {
// Compare this snippet from java/cc/unitmesh/untitled/demo/entity/MeetingRoom.java:
// public class MeetingRoom {
// Compare this snippet from java/cc/unitmesh/untitled/demo/controller/BlogController.java:
//     @ApiOperation(value = "Create a new blog")
// Compare this snippet from java/cc/unitmesh/untitled/demo/controller/BlogControllerTest.java:
// class BlogControllerTest {
// Compare this snippet from java/cc/unitmesh/untitled/demo/DemoApplicationTests.java:
// class DemoApplicationTests {
// Compare this snippet from java/cc/unitmesh/untitled/demo/service/BlogService.java:
// public class BlogService {
// Compare this snippet from java/cc/unitmesh/untitled/demo/dto/CreateBlogRequest.java:
// public class CreateBlogRequest {
// Compare this snippet from java/cc/unitmesh/untitled/demo/controller/BlogCategoryController.java:
// public class BlogCategoryController {
// Compare this snippet from java/cc/unitmesh/untitled/demo/dto/MeetingRoomDetailsResponse.java:
// public class MeetingRoomDetailsResponse { Compare this snippets: //  class CreateBlogRequest {
//    title
//    content
//    
//  }//  class BlogPost {
//    id
//    title
//    content
//    author
//    + public BlogPost(String title, String content, String author)
//    + public BlogPost()
//    + public Long getId()
//    + public String getTitle()
//    + public void setTitle(String title)
//    + public String getContent()
//    + public void setContent(String content)
//    + public String getAuthor()
//    + public void setAuthor(String author)
//  }

\`\`\`Java
// create blog
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
\`\`\`
```