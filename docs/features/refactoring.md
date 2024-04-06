---
layout: default
title: Refactoring
parent: Basic Features
nav_order: 11
permalink: /features/refactoring
---

AutoDev@1.8.0
{: .label .label-yellow }

In [#129](https://github.com/unit-mesh/auto-dev/issues/129), we provide more information for you to refactor code.

## Auto smell combination

It Will provide more information for you to refactor code. like:

- Highlighter issue in IntelliJ IDEA

For example:

```java
public BlogPost updateBlog(Long id, BlogPost blogDto) {
        String content = blogDto.getContent();
        return blogRepository.findById(id).map(blog -> {
            blog.setTitle(blogDto.getTitle());
            blog.setContent(blogDto.getContent());
            return blogRepository.save(blog);
        }).orElse(null);
    }

// relative static analysis result:
// - Variable 'content' is never used
```

## Suggestions for Next Steps

We provide most common suggestions for refactoring code, after you execute refactor action, you will see the following suggestions:

```
intentions.refactor.readability=Please use concise and meaningful variable, function, and class names to improve code readability.
intentions.refactor.usability=Please ensure proper indentation and formatting to enhance code structure and readability.
intentions.refactor.performance=Please optimize algorithms and data structures to improve code performance.
intentions.refactor.maintainability=Please refactor long and complex functions into smaller, more manageable ones to improve code maintainability.
intentions.refactor.flexibility=Please design the system to be flexible and easily adaptable to changing requirements.
intentions.refactor.reusability=Please design and implement reusable components or modules to reduce duplication and improve development efficiency.
intentions.refactor.accessibility=Please consider accessibility requirements and design the system to be usable by all users, regardless of their abilities.
```

## Naming Suggestions

In [#132](https://github.com/unit-mesh/auto-dev/issues/132), we provide basic functionality for renaming things.

1. enable suggestion: `Settings` -> `AutoDev` -> `AutoDev Coder` -> `Enable Rename suggestion`
2. select the variable you want to rename use `Shift` + `F6`

<img src="https://unitmesh.cc/auto-dev/autodev-rename.png" alt="Rename Functions" width="600px"/>

## Resource

In [How to Refactor this Code? An Exploratory Study on Developer-ChatGPT Refactoring Conversations](https://arxiv.org/abs/2402.06013) ,
talking about how to refactoring to better understand how developers identify areas for improvement in code
and how ChatGPT addresses developers' needs. 
