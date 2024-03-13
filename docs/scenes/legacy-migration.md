---
layout: default
title: Legacy Migration
nav_order: 1
parent: Scenes
---
## AI-Assisted Legacy System Migration

Building upon the experience accumulated in our "[System Refactoring and Migration Guide](https://migration.ink/)," we have developed a series of AI capabilities within AutoDev to aid developers in accelerating the transformation of legacy systems.

- Assisting in API data generation for migration testing.
- Facilitating knowledge management through document generation.
- Creating living documentation business systems based on annotated documentation.
- Legacy code refactoring with an object-oriented approach.
- Generating PL/SQL code and migrating Java code.

## Generating API Test Data from Existing Code

### 1. Generate API Test Data

Support language: Java.

Screenshot EXAMPLE：

![AutoDev Living Documentation](https://unitmesh.cc/auto-dev/gen-test-data.png)

Simply right-click on the Java Controller code, select "Generate Test Data (APIs)," and API test data will be generated.

### 2. Custom Prompt Integration

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

## Comments and Documentation Generation

Select the corresponding code, right-click, and choose `Generate Documentation` to generate documentation.

## Use [Custom Living Documentation](/custom/living-documentation) for generating dynamic documentation.

Screenshot EXAMPLE：

![AutoDev Living Documentation](https://unitmesh.cc/auto-dev/autodev-living-doc.png)

Configuration: `Settings` -> `Tools` -> `AutoDev` -> `Customize Engine prompt`, add custom formats for living documentation. Example:

```json
{
  "spec": {
  },
  "prompts": [
  ],
  "documentations": [
    {
      "title": "Living Documentation",
      "prompt": "Write Living Documentation. Return in the following format: ",
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

## PL/SQL Migration

AutoDev has been supporting basic PL/SQL migration since version 1.5.5.

1. Select PL/SQL code.
2. Right-click and choose:
   - Generate Entity
   - Generate Test Cases
   - Generate Java Code

Screenshot EXAMPLE：

![SQL Migration](https://unitmesh.cc/auto-dev/autodev-sql-migration.png)
