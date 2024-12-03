---
layout: default
title: Custom Documentation
parent: Customize Features
nav_order: 12
permalink: /custom/living-documentation
---

# Custom Documentation

## Living Documentation example

goto: `Settings` -> `Tools` -> `AutoDev` -> `Customize Engine prompt`, and add your own prompt.

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

**Output**

```java
@ScenarioDescription(
        given = "there is a meeting room available with ID 123",
        when = "a user books the meeting room with ID 123",
        then = "the booking response should contain the details of the booked meeting room"
)
public BookMeetingRoomResponse bookMeetingRoom(@RequestBody BookMeetingRoomRequest request) {
    MeetingRoom meetingRoom = meetingRoomService.bookMeetingRoom(request.getMeetingRoomId());
    BookMeetingRoomResponse response = new BookMeetingRoomResponse();
    BeanUtils.copyProperties(meetingRoom, response);
    return response;
}
```

## Normal Example


```json
{
  "documentations": [
    {
      "title": "生成 Java 方法级别注释",
      "prompt": "生成 JavaDoc 方法级别的注释，只返回方法的注释。\n要求：1. 需要使用写明方法的使用法。2. 只返回方法的注释，使用注释符号 /** 开头\n\n",
      "start": "",
      "end": "",
      "type": "normal",
      "example": {
        "question": "    public void addEdge(int a, int b) {\n        graph.get(a).add(b);\n        graph.get(b).add(a); // Assuming an undirected graph\n    }",
        "answer": "    /**\n     * 向图中添加一条边。\n     *\n     * <pre>\n     *    {@code\n     *        Calculate graph = new Calculate(5); // 创建一个具有5个节点的图\n     *        graph.addEdge(0, 1); // 在节点0和节点1之间添加边\n     *        // 假定这是一个无向图，因此边(0,1)和边(1,0)都会被添加\n     *    }\n     * </pre>\n     *\n     * @param a 节点A的索引\n     * @param b 节点B的索引\n     *          两个节点都应在添加边之前先添加到图中\n     */\n"
      }
    }
  ]
}
```


**Output**

```java
    /**
     * 向图中添加一条边。
     *
     * <pre>
     *    {@code
     *        Calculate graph = new Calculate(5); // 创建一个具有5个节点的图
     *        graph.addEdge(0, 1); // 在节点0和节点1之间添加边
     *        // 假定这是一个无向图，因此边(0,1)和边(1,0)都会被添加
     *    }
     * </pre>
     *
     * @param a 节点A的索引
     * @param b 节点B的索引
     *          两个节点都应在添加边之前先添加到图中
     */

```
