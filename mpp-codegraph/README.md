# MPP CodeGraph

A Kotlin Multiplatform library for parsing source code and building code graphs using TreeSitter.

## Build Status

✅ **Build Successful** - All tests passing

### Quick Commands

```bash
# Build the module
./gradlew :mpp-codegraph:build

# Run all tests
./gradlew :mpp-codegraph:allTests

# Run JVM tests only
./gradlew :mpp-codegraph:jvmTest

# Run JS tests only
./gradlew :mpp-codegraph:jsTest
```

## Overview

MPP CodeGraph provides a unified API for parsing source code across different platforms (JVM and JS) using TreeSitter parsers. It extracts code structure information (classes, methods, fields, etc.) and relationships (inheritance, composition, dependencies) to build a comprehensive code graph.

## Features

- **Multiplatform Support**: Works on JVM and JavaScript platforms
- **TreeSitter-based Parsing**: Uses TreeSitter for accurate and fast parsing
- **Language Support**: Java, Kotlin, C#, JavaScript, TypeScript, Python, Go, Rust
- **Code Graph Model**: Unified data model for code nodes and relationships
- **Type-safe API**: Kotlin-first design with full type safety

## Architecture

### Common Code (`commonMain`)

The common code defines the core data models and interfaces:

- **Model Classes**:
  - `CodeNode`: Represents a code element (class, method, field, etc.)
  - `CodeRelationship`: Represents relationships between code elements
  - `CodeGraph`: Container for nodes and relationships
  - `CodeElementType`: Enum for different code element types
  - `RelationshipType`: Enum for different relationship types

- **Parser Interface**:
  - `CodeParser`: Common interface for parsing code
  - `Language`: Enum for supported programming languages

### JVM Implementation (`jvmMain`)

Uses TreeSitter Java bindings from `io.github.bonede`:

- **Dependencies**:
  - `tree-sitter:0.25.3`
  - `tree-sitter-java:0.23.4`
  - `tree-sitter-kotlin:0.3.8.1`
  - `tree-sitter-c-sharp:0.23.1`

- **Implementation**:
  - `JvmCodeParser`: JVM-specific parser implementation
  - Based on SASK project architecture

### JS Implementation (`jsMain`)

Uses web-tree-sitter for browser and Node.js:

- **Dependencies**:
  - `web-tree-sitter:0.22.2`
  - `@unit-mesh/treesitter-artifacts:1.7.3`

- **Implementation**:
  - `JsCodeParser`: JavaScript-specific parser implementation
  - Based on autodev-workbench architecture

## Usage

### Basic Usage

```kotlin
import cc.unitmesh.codegraph.CodeGraphFactory
import cc.unitmesh.codegraph.parser.Language

// Create a parser instance
val parser = CodeGraphFactory.createParser()

// Parse a single file
val sourceCode = """
    package com.example;
    
    public class HelloWorld {
        public void sayHello() {
            System.out.println("Hello");
        }
    }
""".trimIndent()

val nodes = parser.parseNodes(sourceCode, "HelloWorld.java", Language.JAVA)

// Parse multiple files and build a graph
val files = mapOf(
    "HelloWorld.java" to sourceCode1,
    "Greeter.java" to sourceCode2
)

val graph = parser.parseCodeGraph(files, Language.JAVA)

// Query the graph
val classes = graph.getNodesByType(CodeElementType.CLASS)
val relationships = graph.getRelationshipsByType(RelationshipType.MADE_OF)
```

### Platform-Specific Usage

#### JVM

```kotlin
import cc.unitmesh.codegraph.parser.jvm.JvmCodeParser

val parser = JvmCodeParser()
val nodes = parser.parseNodes(sourceCode, filePath, Language.JAVA)
```

#### JavaScript/Node.js

```kotlin
import cc.unitmesh.codegraph.parser.js.JsCodeParser

val parser = JsCodeParser()
parser.initialize() // Initialize TreeSitter
val nodes = parser.parseNodes(sourceCode, filePath, Language.JAVASCRIPT)
```

## Building

### Build All Platforms

```bash
./gradlew :mpp-codegraph:build
```

### Build JVM Only

```bash
./gradlew :mpp-codegraph:jvmTest
```

### Build JS Only

```bash
./gradlew :mpp-codegraph:jsTest
```

### Assemble JS Package

```bash
./gradlew :mpp-codegraph:assembleJsPackage
```

## Testing

Run tests for all platforms:

```bash
./gradlew :mpp-codegraph:allTests
```

Run JVM tests only:

```bash
./gradlew :mpp-codegraph:jvmTest
```

Run JS tests only:

```bash
./gradlew :mpp-codegraph:jsTest
```

## Version Information

### TreeSitter Versions

**JVM (io.github.bonede)**:
- tree-sitter: 0.25.3
- tree-sitter-java: 0.23.4
- tree-sitter-kotlin: 0.3.8.1
- tree-sitter-csharp: 0.23.1

**JS (npm packages)**:
- web-tree-sitter: 0.22.2
- @unit-mesh/treesitter-artifacts: 1.7.3
  - tree-sitter-java: 0.21.0
  - tree-sitter-kotlin: 0.3.8
  - tree-sitter-c-sharp: 0.20.0

## Design Principles

1. **Platform Abstraction**: Common interfaces with platform-specific implementations
2. **Consistent API**: Same API across all platforms
3. **Version Alignment**: TreeSitter versions aligned with reference projects (SASK and autodev-workbench)
4. **Type Safety**: Full Kotlin type safety with serializable models
5. **Extensibility**: Easy to add new languages and relationship types

## References

- **SASK Project**: JVM implementation reference
- **autodev-workbench**: JS implementation reference
- **TreeSitter**: https://tree-sitter.github.io/tree-sitter/

## License

MIT License

