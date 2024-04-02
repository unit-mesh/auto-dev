---
layout: default
title: Executable Unit Validation
nav_order: 4
parent: Design Patterns
---

Executable unit validation refers to the process of testing and verifying the generated code to ensure that it can be
compiled and executed by the compiler. The aim of executable unit validation is to enhance the accuracy and
executability of the generated code, ensuring that the generated code units meet expectations and can be effectively
utilized.

In AutoDev, corresponding to functionalities such as SQL combined with the database, unit testing, and functional code
generation, we have preliminarily designed the following validation mechanisms:

- Unit Test Syntax (TODO): Check whether the generated unit test code complies with language syntax specifications to
  ensure it can be compiled correctly by the compiler.
- Unit Test Execution: Execute the generated unit test cases to test the generated code, ensuring it can be compiled and
  executed correctly by the compiler.
- SQL Syntax Validation: Generate SQL statements based on different model capabilities and handle any resulting errors.
- SQL Schema Validation (TODO): Combine with the connected database to check the generated SQL statements, ensuring they
  comply with the database schema specifications.
- Functional Code Generation Validation (TODO): Adopt a test-driven validation mechanism to check the generated code,
  ensuring it meets development requirements and can be compiled correctly by the compiler.
- Frontend Code Generation Validation (TODO): Check the generated frontend code to ensure correctness in imports,
  syntax, etc.

Considering that unit tests are directly executable, in AutoDev, we directly execute unit tests (`RunService`), and with
a fast enough IDE, the validation process is generally rapid. Through the aforementioned validation mechanisms, we can
effectively enhance the accuracy and executability of the generated code, ensuring that the generated code units meet
expectations and can be effectively utilized.