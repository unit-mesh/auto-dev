---
layout: default
title: Agentic Testcase
nav_order: 998
parent: Development
---

## AutoDev Sketch

### Alpha 1

- git 如何 merge branch。
- 添加删除博客
```devin
/file:src/main/java/cc/unitmesh/untitled/demo/controller/BlogController.java
/file:src/main/java/cc/unitmesh/untitled/demo/service/BlogService.java
/file:src/main/java/cc/unitmesh/untitled/demo/repository/BlogRepository.java
```
- 添加根据作者删除博客
- 给 Blogpost 添加 category 的功能，并支持按 category 获取所有的 blog
```devin
/file:src/main/java/cc/unitmesh/untitled/demo/controller/BlogController.java
/file:src/main/java/cc/unitmesh/untitled/demo/service/BlogService.java
/file:src/main/java/cc/unitmesh/untitled/demo/repository/BlogRepository.java
```
- 采用 DDD，生成对应的 ApplicationService 和 DomainService
```devin
/file:src/main/java/cc/unitmesh/untitled/demo/service/BlogService.java
/file:src/main/java/cc/unitmesh/untitled/demo/repository/BlogRepository.java
```

### Alpha 2

- 采用 DDD + 充血模型，重构 Blog 功能
- Gradle 启动Spring boot 应用的 bash 代码
- 获取最近两周的代码提交，生成发布日志
- 针对代码变更，生成测试

### Multiple-Language

- 使用 patch 的方式添加 vue router
- 使用 patch 的方式添加 Element UI
- 生成 OpenAPI Yaml 示例，使用 markdown yaml 返回
- 编写 go hello world，使用 markdown go 语言返回（不使用 write 等 DevIns 指令）
- 我在为当前项目创建 OpenAPI 3.0 spec，编写一个基本的 swagger yaml 骨架，方便我学习。只返回 swagger yaml，不使用 devins
- 根据如下 Vue 数据结构，设计重构策略：
```devin
/structure:src/components/Custom/DataSelect.vue
```
- 结合行号，使用 file 指令选择合适的“代码段”来查看代码。由于代码太长，你只能选择一小段。
- 我只想要一个生成 Vite + Jest 的 package.json 示例，再添加 axios、jszip的依赖。

## AutoDev Bridge

### 切换数据库到 MongoDB

### Vue2 迁移到 Vue 3

### Spring Boot to micronaut migration

Spring Boot to micronaut migration
```devin
/dependencies
/scc
/containerView
/dir:src
/ripgrepSearch:JdbcTemplate
/database:schema
```

### 使用 Node.js 重写当前项目

如下是当前项目的 APIs
```devin
/webApiView
```
