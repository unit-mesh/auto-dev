---
layout: default
title: Legacy Migration
nav_order: 1
parent: Scenes
permalink: /scenes/legacy-migration
---

Scene: Legacy Migration

Steps:

1. Generate API documentation
2. Generate API test cases from legacy code or API documentation
3. Record API data or Generate API test data
4. Refactor and modularize the legacy code
5. Transpile the legacy code to the new code

## Generate API documentation

use [Custom Living documentation](/custom/living-documentation) to generate API documentation.

goto: `Settings` -> `Tools` -> `AutoDev` -> `Customize Engine prompt`, and add your own prompt. For example:

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

## Generate API test cases from legacy code or API documentation

use [Custom Action](/custom/action) to generate API test cases from legacy code or API documentation.

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