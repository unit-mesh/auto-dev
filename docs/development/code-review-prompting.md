---
layout: default
title: Code Review
nav_order: 12
parent: Development
---

## Code Review

### Sample

link: https://github.com/domvwt/chatgpt-code-review/blob/main/chatgpt_code_review/query.py

   Please review the code below and identify any syntax or logical errors, suggest
   ways to refactor and improve code quality, enhance performance, address security
   concerns, and align with best practices. Provide specific examples for each area
   and limit your recommendations to three per category.
   
   Use the following response format, keeping the section headings as-is, and provide
   your feedback. Use bullet points for each response. The provided examples are for
   illustration purposes only and should not be repeated.
   
   **Syntax and logical errors (example)**:
   - Incorrect indentation on line 12
   - Missing closing parenthesis on line 23
   
   **Code refactoring and quality (example)**:
   - Replace multiple if-else statements with a switch case for readability
   - Extract repetitive code into separate functions
   
   **Performance optimization (example)**:
   - Use a more efficient sorting algorithm to reduce time complexity
   - Cache results of expensive operations for reuse
   
   **Security vulnerabilities (example)**:
   - Sanitize user input to prevent SQL injection attacks
   - Use prepared statements for database queries
   
   **Best practices (example)**:
   - Add meaningful comments and documentation to explain the code
   - Follow consistent naming conventions for variables and functions

## Final
   
   You are a seasoned software developer, and I'm seeking your expertise to review the following code:
   
   - Focus on critical algorithms, logical flow, and design decisions within the code. Discuss how these changes impact the core functionality and the overall structure of the code.
   - Identify and highlight any potential issues or risks introduced by these code changes. This will help reviewers pay special attention to areas that may require improvement or further analysis.
   - Emphasize the importance of compatibility and consistency with the existing codebase. Ensure that the code adheres to the established standards and practices for code uniformity and long-term maintainability.
     
     You are working on a project that uses Spring MVC,Spring WebFlux,JDBC to build business logic.
   
     The following user stories are related to these changes:
     预定会议室
     Commit Message: feat(meeting): add repository #6\n\nCode Changes:\n\nIndex: src/main/java/cc/unitmesh/untitled/demo/repository/MeetingRepository.java
     new file mode 100644
     --- /dev/null
     +++ b/src/main/java/cc/unitmesh/untitled/demo/repository/MeetingRepository.java
     @@ -0,0 +1,10 @@
     +package cc.unitmesh.untitled.demo.repository;
   
   +import cc.unitmesh.untitled.demo.entity.BlogPost;
   +import org.springframework.data.repository.CrudRepository;
   +import org.springframework.stereotype.Repository;
   +
   +@Repository
   +public interface MeetingRepository extends CrudRepository<BlogPost, Long> {
   +
   +}
   As your Tech lead, I am only concerned with key code review issues. Please provide me with a critical summary.
   Submit your key insights under 5 sentences in here:

## Commit Message Generate 

### Strategy

- filter key patch message.
- combine with commit message and code diff.
- find history commit message as examples.
