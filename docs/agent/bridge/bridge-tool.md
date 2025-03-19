---
layout: default
title: Bridge Tools
parent: AutoDev Bridge - Legacy Migration
nav_order: 1
permalink: /bridge/bridge-tool
---

## AutoDev Bridge Tools 

| 工具名称 (name)   | 描述 (desc)                                                       | 示例 (example)                                                                                                                                                                    |
|---------------|-----------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| componentView | 列出当前项目的所有UI组件列表，如React、Vue组件                                    | `<devin>`<br>List all UI Component List of current project, like React Vue components<br>`/componentView`<br>`</devin>`                                                         |
| containerView | 列出当前项目的所有模块                                                     | `<devin>`<br>List all modules of current project<br>`/containerView`<br>`</devin>`                                                                                              |
| scc           | Scc 是一个非常快速且准确的代码计数器，具有复杂度计算和COCOMO估算功能                         | `<devin>`<br>Scc is a very fast accurate code counter with complexity calculations and COCOMO estimates<br>`/scc`<br>`</devin>`                                                 |
| history       | 获取当前文件的历史提交信息                                                   | `<devin>`<br>Get history commit message of current file<br>`/history:package.json`<br>`</devin>`                                                                                |
| knowledge     | 从 API 调用链进行分析，默认 depth = 2（不可修改），即 Controller 到 Repository 的调用链 | `<devin>`<br>从 API 调用链来进行分析，默认 depth = 2（不可修改），即 Controller 到 Repository 的调用链<br>`/knowledge:GET#/api/blog/*` [注:这里 * 代表 blog slug，等同于 SpringMVC 的 @PathVariable]<br>`</devin>` |
| database      | 数据库模式和查询工具                                                      | `<devin>`<br>列出数据库的数据结构<br>`/database:schema`<br>列出数据库中的所有表<br>`/database:table`<br>`</devin>`                                                                                  |
| stylingView   | 列出当前项目的所有CSS、SCSS类                                              | `<devin>`<br>List all CSS, SCSS classes of current project<br>`/stylingView`<br>`</devin>`                                                                                      |
| dependencies  | 获取当前项目的所有依赖项（Gradle、Maven、package.json）                         | `<devin>`<br>Get all dependencies (Gradle, Maven, package.json) of current project<br>`/dependencies`<br>`</devin>`                                                             |
| webApiView    | 列出当前项目的所有Web API                                                | `<devin>`<br>List all web apis of current project<br>`/webApiView`<br>If return no endpoints, we need to check Endpoint plugin installed.<br>`</devin>`                         |