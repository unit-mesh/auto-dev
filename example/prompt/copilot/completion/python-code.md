Q:
```python
def foo():
    x = 42 + 42
    print(x)
    # TODO: write me gcd algorithm here
    # end of the code completion
    return x + x
```
A:
```python
def foo():
    x = 42 + 42
    print(x)
    # TODO: write me gcd algorithm here
    def gcd(a,b):
        while(b):
            a,b = b,a%b
        return a
    # end of the code completion
    return x + x
```
Q: