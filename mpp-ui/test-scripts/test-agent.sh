#!/bin/bash

# Test AutoDev CLI Agent
# This script tests the coding agent with a simple task

set -e

echo "🧪 Testing AutoDev CLI Agent..."
echo ""

# Test project path
TEST_PROJECT="/Users/phodal/IdeaProjects/untitled"

# Simple test task
TEST_TASK="Create a simple Hello.java file in src directory with a main method that prints 'Hello, AutoDev!'"

echo "📁 Test Project: $TEST_PROJECT"
echo "📝 Test Task: $TEST_TASK"
echo ""

# Run the agent
node dist/index.js code \
  -p "$TEST_PROJECT" \
  -t "$TEST_TASK"

echo ""
echo "✅ Test completed!"

