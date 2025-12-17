# 编码风格约定

## 编码习惯
- **依赖基线**：以 Maven + Java11（可使用更高版本 JDK 编译）为基线；统一使用 Lombok 注解并通过 `log.*` 输出日志。
- **依赖最小化**：能用 JDK/Netty 自带能力解决的问题，不新增第三方依赖；确需引入时说明原因，并优先封装在 `util/` 或独立组件中，避免业务代码散落直接调用。
- **常量/魔数**：常量统一放在 `cn.jascript.zt808.constants`，业务代码不得直接写魔数。
- **判空与三元**：判空使用 `Objects.isNull/Objects.nonNull`；三元表达式整体加括号，避免阅读歧义。
- **局部变量**：倾向使用 `var` 进行类型推断，除非语义需要显式类型。
- **Fast-Fail**：条件判断优先采用 fast-fail（提前 return/continue），确保主干逻辑清晰。
- **大括号**：单行分支允许省略 `{}`，但条件和语句之间需要换行同时若增加第二条语句必须补齐，保持一致性。
- **Netty 规范**：操作 `ByteBuf` 时遵守 `duplicate()/retain()` 等引用规则，并用 `try/finally` 释放临时 buffer；Pipeline 始终保持“解码 → 业务 → 编码”顺序，Handler 继承 `ChannelInboundHandlerAdapter`，超时逻辑通过工厂注入。
- **Netty Handler 复用**：解码器（`ByteToMessageDecoder`）必须按连接创建，不可复用；若要复用单例 Handler，需要保证无状态并标注 `@ChannelHandler.Sharable`。
- **可读性优先**：变量/方法命名使用非英文母语者也易理解的词汇；必要时可增加中间变量或临时变量以提升语义清晰度。
- **工具使用**：通用逻辑封装到 util 包，业务层仅调用工具方法，不重复实现；若需第三方库，优先在 util 内聚合。
- **日志级别**：统一使用 INFO/DEBUG/ERROR；DEBUG 仅用于排查问题，INFO 记录关键流程，ERROR 须包含完整上下文(比如ip、port、terminalId等)。
- **异常处理**：避免大段 `try/catch`，部分频繁出现的捕获可以考虑拆出业务方法；仅对 `Thread.sleep` 等语义明确、对异常内容不敏感的场景使用 `catch (Exception ignore)` 模式，其他情况需补充日志或状态处理。
- **不可变集合**：倾向使用 `List.of`、`Map.of`、`Set.of` 构建只读集合，表达“无需修改”的语义。
- **会话语义**：会话活跃时间在统一入口刷新（如 `DataEventHandler`），业务 parser 不重复刷新；`terminalId -> channel` 绑定仅在鉴权成功/确认上线后建立；会话状态更新需保证顺序正确（先确保会话存在，再更新状态）。
- **Lambda/Stream**：鼓励用简洁 lambda/stream 实现过滤、转换；多语句块需保持逻辑单一，避免stream过长,必要时可以封装到util
- **条件表达式**：简单分支优先使用 `if`，如if后只有一行逻辑,可以不写大括号,但必须换行,仅函数体只有单一功能时使用switch(如枚举转发)
- **标点**：代码与文档一律使用英文标点，避免中英文标点混用。

---
## 注释
- 关键协议逻辑用简洁中文注释解释背景（BCC、0x7E/0x7D 转义、报文长度等）。
- 新增 Handler、util 或 Pipeline 环节时同步补充注释，保持可维护性。
