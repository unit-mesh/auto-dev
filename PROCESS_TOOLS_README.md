# Process Management Tools

This document describes the new process management tools integrated into the AutoDev IntelliJ plugin, implementing issue #430.

## Overview

The Process Management Tools provide a comprehensive API for managing external processes within IntelliJ IDEA, inspired by Cursor's tool schema approach. These tools allow you to launch, monitor, control, and interact with external processes seamlessly.

## Available Commands

### 1. Launch Process (`/launch-process`)

Launch a new process with specified command and options.

**Syntax:**
```
/launch-process:[options]
```bash
command to execute
```

**Options:**
- `--wait` - Wait for process completion before returning
- `--timeout=N` - Set timeout in seconds (default: 30)
- `--working-dir=PATH` - Set working directory
- `--env=KEY=VALUE` - Set environment variables
- `--show-terminal` - Show process in terminal

**Examples:**

Launch and wait:
```
/launch-process:--wait --timeout=60
```bash
./gradlew build
```

Launch in background:
```
/launch-process:
```bash
npm run dev
```

### 2. List Processes (`/list-processes`)

List all active and terminated processes.

**Syntax:**
```
/list-processes:[options]
```

**Options:**
- `--include-terminated` or `--all` - Include terminated processes
- `--max-results=N` - Limit number of results (default: 50)

**Example:**
```
/list-processes:--all --max-results=20
```

### 3. Kill Process (`/kill-process`)

Terminate a running process by its process ID.

**Syntax:**
```
/kill-process:PROCESS_ID [--force]
```

**Options:**
- `--force` - Force kill the process

**Example:**
```
/kill-process:proc_1234567890_1 --force
```

### 4. Read Process Output (`/read-process-output`)

Read stdout and stderr output from a process.

**Syntax:**
```
/read-process-output:PROCESS_ID [options]
```

**Options:**
- `--stdout-only` - Read only stdout
- `--stderr-only` - Read only stderr
- `--no-stdout` - Exclude stdout
- `--no-stderr` - Exclude stderr
- `--max-bytes=N` - Limit output size (default: 10000)

**Example:**
```
/read-process-output:proc_1234567890_1 --max-bytes=5000
```

### 5. Write Process Input (`/write-process-input`)

Write input data to a running process's stdin.

**Syntax:**
```
/write-process-input:PROCESS_ID [--no-newline]
```text
input data
```

**Options:**
- `--no-newline` - Don't append newline to input

**Example:**
```
/write-process-input:proc_1234567890_1
```
hello world
```

## Architecture

### Core Components

1. **ProcessInfo** - Data class containing process information
2. **ProcessStateManager** - Service for managing process states and lifecycle
3. **ProcessStatus** - Enum representing process states (RUNNING, COMPLETED, FAILED, KILLED, TIMED_OUT)
4. **InsCommand Implementations** - Individual command implementations for each tool

### Process Lifecycle

```
Launch → Running → [Input/Output Operations] → Terminated
```

### Integration Points

- **IntelliJ Process Management APIs** - Leverages existing IntelliJ process handling
- **DevIns Language** - Integrated with the existing command system
- **Tool Registry** - Registered as standard built-in commands

## Usage Patterns

### 1. Build and Test Workflow

```
# Launch build process
/launch-process:--wait --timeout=300
```bash
./gradlew clean build
```

# Check if any processes are still running
/list-processes

# Read build output if needed
/read-process-output:proc_xxx
```

### 2. Development Server Management

```
# Start development server in background
/launch-process:--env=NODE_ENV=development
```bash
npm run dev
```

# List running processes
/list-processes

# Kill server when done
/kill-process:proc_xxx
```

### 3. Interactive Process Communication

```
# Launch interactive process
/launch-process:
```bash
python3 -i
```

# Send commands to Python REPL
/write-process-input:proc_xxx
```
print("Hello from AutoDev!")
```

# Read output
/read-process-output:proc_xxx
```

## Error Handling

- **Process Not Found** - Commands validate process existence
- **Permission Errors** - Proper error messages for access issues
- **Timeout Handling** - Configurable timeouts with clear feedback
- **Resource Cleanup** - Automatic cleanup of terminated processes

## Security Considerations

- **Command Validation** - Input sanitization for shell commands
- **Working Directory Restrictions** - Limited to project scope
- **Environment Variable Control** - Controlled environment variable access
- **Process Isolation** - Processes run in isolated contexts

## Future Enhancements

- **Process Groups** - Support for managing related processes
- **Output Streaming** - Real-time output streaming for long-running processes
- **Process Dependencies** - Define process startup dependencies
- **Resource Monitoring** - CPU and memory usage tracking
- **Persistent State** - Process state persistence across IDE sessions

## Implementation Details

The implementation follows the existing AutoDev architecture:

- **BuiltinCommand** entries for each tool
- **InsCommand** implementations for execution logic
- **InsCommandFactory** registration for command creation
- **Service-level** process state management
- **Example files** for documentation and auto-completion

All process management operations are implemented as Kotlin coroutines for non-blocking execution and integrate seamlessly with IntelliJ's existing process management APIs.
