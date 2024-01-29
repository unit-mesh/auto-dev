---
layout: default
title: AI 辅助遗留系统改造
nav_order: 1
parent: Scenes
---

## AI 辅助遗留系统改造

基于我们在《[系统重构与迁移指南](https://migration.ink/)》沉淀的经验，我们在 AutoDev 中构建了一系列的 AI 能力，帮助开发者更快地进行遗留系统改造。

- 辅助迁移测试的 API 数据生成。
- 辅助知识管理的文档生成。
- 基于注释文档的活文档业务体系。
- 面向对象的遗留代码重构。
- PL/SQL 代码生成与迁移 Java 代码。

## 从已有代码生成 API 测试数据

默认支持 Spring 框架

右键 Java 的 Controller代码，选择 `Generate Test Data (APIs)`，即可生成 API 测试数据。

### 通过自定义 Prompt

goto: `Settings` -> `Tools` -> `AutoDev` -> `Customize Engine prompt`, and add your own prompt. For example:

```json
{
  "spec": {
  },
  "prompts": [
    {
      "title": "Generate API test cases",
      "autoInvoke": false,
      "matchRegex": ".*",
      "priority": 0,
      "template": "Generate API testcases based on following information: \n${METHOD_INPUT_OUTPUT}\nHere is the code:\n${SELECTION}"
    }
  ],
  "documentations": []
}
```

## 注释与文档生成

选中对应的代码，右键选择 `Generate Documentation`，即可生成文档。

## 使用 [Custom Living documentation](/custom/living-documentation) 生成活文档。

配置: `Settings` -> `Tools` -> `AutoDev` -> `Customize Engine prompt`, 添加自定义的活文档格式。示例:

```json
{
  "spec": {
  },
  "prompts": [
  ],
  "documentations": [
    {
      "title": "Living Documentation",
      "prompt": "编写 Living Documentation。按如下的格式返回：",
      "start": "",
      "end": "",
      "type": "annotated",
      "example": {
        "question": "public BookMeetingRoomResponse bookMeetingRoom(@RequestBody BookMeetingRoomRequest request) {\n        MeetingRoom meetingRoom = meetingRoomService.bookMeetingRoom(request.getMeetingRoomId());\n        BookMeetingRoomResponse response = new BookMeetingRoomResponse();\n        BeanUtils.copyProperties(meetingRoom, response);\n        return response;\n    }",
        "answer": "    @ScenarioDescription(\n        given = \"there is a meeting room available with ID 123\",\n        when = \"a user books the meeting room with ID 123\",\n        then = \"the booking response should contain the details of the booked meeting room\"\n    )"
      }
    }
  ]
}
```

## PL/SQL 迁移

AutoDev 自 1.5.5 版本开始支持基本的 PL/SQL 迁移。

1. 选择 PL/SQL 代码
2. 右键选择
    - 生成 Entity
    - 生成测试用例
    - 生成 Java Code
