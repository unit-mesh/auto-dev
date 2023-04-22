1. 使用 HTTP 状态码
    * 200 OK: 请求成功
    * 201 Created: 创建资源成功
    * 204 No Content: 更新/删除操作成功,无内容返回
    * 400 Bad Request: 无效的请求语法
    * 404 Not Found: 无法找到请求的资源
    * 409 Conflict: 资源冲突,更新/创建操作失败
    * 422 Unprocessable Entity: 无法处理资源,通常是参数验证失败
    * 500 Internal Server Error: 服务内部错误
2. 使用 Content-Type 来区分不同的返回内容
    * application/json; charset=utf-8: JSON 响应体
    * text/plain; charset=utf-8: 简单文本响应体
3. 使用统一的时间戳格式
    * ISO 8601 标准,例如:2019-06-27T15:25:43Z
4. 避免 MIME 类型的猜测
    * 明确设置 Content-Type,避免MIME 类型猜测带来的问题
5. 异常使用标准的 HTTP 状态码返回
    * 不使用非标准的状态码,以免客户端无法正常解析
6. 所有非字符串数据使用 ISO 8601 时间戳格式
    * 避免时间数据的解析问题
7. 所有的数值数据使用十进制 (base 10) format
    * 避免数据格式解析的问题
8. 所有 Geo 位置数据使用 WGS 84 坐标系
    * 以保证 Geo 位置数据解析和使用的一致性
9. 所有 ID 使用唯一递增整型
    * 避免 ID 冲突,更容易维护和扩展
10. API 文档要简明扼要
     * 增加注释来解释不太直观的设计
     * 避免过度复杂的 API,可扩展性好,设计简单易用
