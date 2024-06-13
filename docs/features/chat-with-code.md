---
layout: default
title: Chat with code
parent: Basic Features
nav_order: 4
permalink: /features/chat-width-code
---

1. Select a code fragment and right-click it to open the context menu. (AutoDev Chat)
2. Select AI Actions 

<img src="https://unitmesh.cc/auto-dev/chat-with-code.png" alt="Chat code completion" width="600px"/>

## Normal Chat

1. Click AutoDev on the right toolbar to open AutoDev.
2. In the input field, type your question and press `Shift` + `Enter` to submit your question.

<img src="https://unitmesh.cc/auto-dev/normal-chat.png" alt="Normal Chat" width="600px"/>

## Explain this

## Refactor this

In [#129](https://github.com/unit-mesh/auto-dev/issues/129), we provide more information for you to refactor code.

## Chat with this

## Write test for this

## Generate test data (APIs)

support language: Java

1. right-click the function/method and selection `Generate test data (APIs)`

AutoDev with analysis input and output data structure

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

convert it to uml

```plantuml
//input Classes: 
class CreateBlogRequest {
  title: String
  content: String
  User: User
}

class User {
  id: Long
  name: String
}

//output Class: 
class BlogPost {
  id: Long
  title: String
  content: String
  author: String
}
```

AI will gen to

```json
{
  "title": "Sample Blog",
  "content": "Lorem ipsum dolor sit amet, consectetur adipiscing elit.",
  "User": {
    "id": 1,
    "name": "John Doe"
  }
}
```