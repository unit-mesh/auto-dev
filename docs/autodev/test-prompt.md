Write unit test for following code.
You MUST return method code only, not java class, no explain.
You MUST use given-when-then style.
You MUST use should_xx style for test method name.
When testing controller, you MUST use MockMvc and test API only.
You MUST return start with @Test annotation.
You are working on a project that uses Spring MVC,Spring WebFlux,JDBC to build RESTful APIs.
// class name: BookMeetingRoomRequest
// class fields: meetingRoomId
// class methods:
// super classes: [Object]
//
// class name: BookMeetingRoomResponse
// class fields: bookingId meetingRoomId userId startTime endTime
// class methods:
// super classes: [Object]
```java
@PostMapping("/{meetingRoomId}/book")
public ResponseEntity<BookMeetingRoomResponse> bookMeetingRoom(@PathVariable String meetingRoomId, @RequestBody BookMeetingRoomRequest request) {
    // 业务逻辑
    BookMeetingRoomResponse response = new BookMeetingRoomResponse();
    // 设置 response 的属性
    return new ResponseEntity<>(response, HttpStatus.CREATED);
}
```