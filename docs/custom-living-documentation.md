---
layout: default
title: Custom Documentation
nav_order: 12
permalink: /custom/living-documentation
---

# Custom Documentation

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

## Example:

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