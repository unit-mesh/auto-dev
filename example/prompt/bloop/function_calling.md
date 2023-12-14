```json
[
  {
    "name": "code",
    "description": "Search the contents of files in a codebase semantically. Results will not necessarily match search terms exactly, but should be related.",
    "parameters": {
      "type": "object",
      "properties": {
        "query": {
          "type": "string",
          "description": "The query with which to search. This should consist of keywords that might match something in the codebase, e.g. 'react functional components', 'contextmanager', 'bearer token'"
        }
      },
      "required": [
        "query"
      ]
    }
  },
  {
    "name": "path",
    "description": "Search the pathnames in a codebase. Results may not be exact matches, but will be similar by some edit-distance. Use when you want to find a specific file or directory.",
    "parameters": {
      "type": "object",
      "properties": {
        "query": {
          "type": "string",
          "description": "The query with which to search. This should consist of keywords that might match a path, e.g. 'server/src'."
        }
      },
      "required": [
        "query"
      ]
    }
  },
  {
    "name": "none",
    "description": "You have enough information to answer the user's query. This is the final step, and signals that you have enough information to respond to the user's query. Use this if the user has instructed you to modify some code.",
    "parameters": {
      "type": "object",
      "properties": {
        "paths": {
          "type": "array",
          "items": {
            "type": "integer",
            "description": "The indices of the paths to answer with respect to. Can be empty if the answer is not related to a specific path."
          }
        }
      },
      "required": [
        "paths"
      ]
    }
  },
  {
    "name": "proc",
    "description": "Read one or more files and extract the line ranges which are relevant to the search terms. Do not proc more than 10 files at a time.",
    "parameters": {
      "type": "object",
      "properties": {
        "query": {
          "type": "string",
          "description": "The query with which to search the files."
        },
        "paths": {
          "type": "array",
          "items": {
            "type": "integer",
            "description": "The indices of the paths to search. paths.len() <= 10"
          }
        }
      },
      "required": ["query", "paths"]
    }
  }
]
```