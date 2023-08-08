# Custom Server API example (ChatGLM2)

Your LLM API example:

```http request
POST http://127.0.0.1:8000/chat
Content-Type: application/json

{
  "instruction": "code complete given code:\n```java public class Main {\n    public static void main(String[] args) {",
  "input":""
}

### Response: 
### "Here's an explanation of the given code:\n\n```\npublic class Main {\n```\nThis line defines the name of the main class as `Main`.\n```\npublic static void main(String[] args) {\n```\nThis line defines the `main` method as the entry point of the program. The `public`, `static`, and `void` access modifiers indicate that this method can be accessed directly from outside the class, it is static in nature and it does not take arguments.\n```\n    String[] args = new String[1];\n    args[0] = \"Hello\";\n    args[1] = \"World\";\n    \n    // Do something with the arguments\n}\n```\nThis line declares a variable `args` of type `String` and initializes it with an array of one element `String` object. The elements of the array are assigned the values \"Hello\" and \"World\" respectively.\n```\n    String[] args = new String[1];\n    args[0] = \"Hello\";\n    args[1] = \"World\";\n    \n    // Do something with the arguments\n}\n```\nThis line declares a variable `args` of type `String` and initializes it with an array of one element `String` object. The elements of the array are assigned the values \"Hello\" and \"World\" respectively.\n```\n    public static void main(String[] args) {\n```\nThis line refers to the `main` method defined in the previous class and passes it the argument array `args`.\n```\n    String[] args = new String[1];\n    args[0] = \"Hello\";\n    args[1] = \"World\";\n    \n    // Do something with the arguments\n}\n```\nThis line refers to the `main` method defined in the previous class and passes it the argument array `args`.\n```\n}\n```\nThis curly brace closes the `main` method.\n```\n}\n```\nThis curly brace closes the `main` method.\n```\n```\n\nOverall, this code creates a variable `args` of type `String` and assigns it an array of one element with the values \"Hello\" and \"World\". The `main` method is called and passes the argument array `args` to it."
```

python code example: [main.py](example/custom_llm_server/main.py)

```python
class MessageInput(BaseModel):
    instruction: str
    input: str

@app.post("/chat")
async def chat(data: MessageInput):
  input = {} # your backend data 

  response = requests.post("your_llm_server", json={
    # your backend data
  }).json()

  return response["data"][0]
```
