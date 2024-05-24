---
layout: default
title: Custom Action
parent: Customize Features
nav_order: 10
permalink: /custom/action
---

# Custom Action

You can customize your prompt in `Settings` -> `Tools` -> `AutoDev`

```json
{
  "spec": {
    "controller": "- Use BeanUtils.copyProperties in the Controller for DTO to Entity conversion.\n- Avoid using Autowired.\n- Use Swagger Annotations to indicate API meanings.\n- Controller methods should capture and handle business exceptions, rather than throwing system exceptions.",
    "service": "- Service layer should use constructor injection or setter injection; avoid using the @Autowired annotation.",
    "entity": "- Entity classes should use JPA annotations for database mapping.\n- The entity class name should match the corresponding database table name. Entity classes should use annotations to mark primary keys and table names, for example: @Id, @GeneratedValue, @Table, etc.",
    "repository": "- Repository interfaces should extend the JpaRepository interface to inherit basic CRUD operations.",
    "ddl": "- Fields should be constrained with NOT NULL constraints to ensure data integrity."
  },
  "prompts": [
    {
      "title": " Code complete",
      "autoInvoke": true,
      "matchRegex": ".*",
      "priority": 1,
      "template": "Code complete:\n${METHOD_INPUT_OUTPUT}\n${SPEC_controller}\n\n${SELECTION}"
    },
    {
      "title": " Translate to Kotlin",
      "autoInvoke": false,
      "matchRegex": ".*",
      "priority": 0,
      "template": "Translate the following code to Kotlin.\n${SIMILAR_CHUNK}\nCompare these snippets:\n${METHOD_INPUT_OUTPUT}\nHere is the code:\n${SELECTION}"
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
- selectedRegex (since @1.8.3 from [#174](https://github.com/unit-mesh/auto-dev/pull/174)): the regex to match the selected code 

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