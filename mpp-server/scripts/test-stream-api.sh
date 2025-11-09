#!/bin/bash

# Test SSE Streaming API
# This script demonstrates how to call the SSE streaming endpoint

set -e

BASE_URL="http://localhost:8080"

echo "🧪 Testing MPP-Server SSE Streaming API"
echo "========================================"
echo ""

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Test 1: Health Check
echo -e "${BLUE}1. Health Check${NC}"
echo "GET $BASE_URL/health"
curl -s "$BASE_URL/health" | jq .
echo ""

# Test 2: Get Projects
echo -e "${BLUE}2. Get Projects${NC}"
echo "GET $BASE_URL/api/projects"
curl -s "$BASE_URL/api/projects" | jq .
echo ""

# Test 3: SSE Streaming (requires a valid project)
echo -e "${BLUE}3. SSE Streaming Agent Execution${NC}"
echo "POST $BASE_URL/api/agent/stream"
echo ""
echo -e "${YELLOW}Note: This will stream events in real-time. Press Ctrl+C to stop.${NC}"
echo ""

# Create request payload
REQUEST_PAYLOAD='{
  "projectId": "autocrud",
  "task": "List all Kotlin files in the mpp-server module",
  "llmConfig": {
    "provider": "openai",
    "modelName": "gpt-4",
    "apiKey": "'"${OPENAI_API_KEY:-sk-test}"'",
    "baseUrl": ""
  }
}'

echo "Request payload:"
echo "$REQUEST_PAYLOAD" | jq .
echo ""

echo -e "${GREEN}Streaming events:${NC}"
echo "---"

# Stream SSE events
curl -N -X POST "$BASE_URL/api/agent/stream" \
  -H "Content-Type: application/json" \
  -d "$REQUEST_PAYLOAD" \
  2>/dev/null | while IFS= read -r line; do
    if [[ $line == event:* ]]; then
        EVENT_TYPE=$(echo "$line" | sed 's/event: //')
        echo -e "${BLUE}[Event: $EVENT_TYPE]${NC}"
    elif [[ $line == data:* ]]; then
        DATA=$(echo "$line" | sed 's/data: //')
        echo "$DATA" | jq . 2>/dev/null || echo "$DATA"
        echo ""
    fi
done

echo "---"
echo -e "${GREEN}✅ Streaming completed${NC}"

