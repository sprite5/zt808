# 项目结构（基于当前代码）

```
 src/
 ├─ main/
 │   ├─ java/
 │   │   └─ cn/jascript/zt808/
 │   │       ├─ boot/
 │   │       │   ├─ Application.java                  # 程序入口
 │   │       │   ├─ Bootstrap.java                    # TCP Server 绑定、Pipeline 装配
 │   │       │   └─ StartupValidator.java             # 启动期配置校验（provider/extProvider 等）
 │   │       ├─ codec/
 │   │       │   ├─ DecoderFactory.java               # 解码器装配工厂（引用于 Bootstrap）
 │   │       │   └─ EncoderFactory.java               # 编码器装配工厂（引用于 Bootstrap）
 │   │       ├─ config/
 │   │       │   └─ AppConfig.java                    # application.yaml 加载（Jackson YAML）+ 扁平化取值
 │   │       ├─ constants/
 │   │       │   ├─ AlarmType.java                    # 报警类型接口
 │   │       │   ├─ StandardAlarmType.java            # 标准报警枚举
 │   │       │   ├─ ExtAlarmType.java                 # 扩展报警枚举（预留）
 │   │       │   ├─ MsgId.java                        # 协议 msgId 常量
 │   │       │   ├─ MsgReplyMode.java                 # 通用应答模式（GENERAL/NONE/PROVIDER）
 │   │       │   ├─ DataType.java                     # DTO 数据类型
 │   │       │   └─ Protocol.java                     # 协议常量（如 0x7E）
 │   │       ├─ forward/
 │   │       │   ├─ ForwardProvider.java              # 转发接口
 │   │       │   ├─ DefaultForwardProvider.java       # 默认转发实现
 │   │       │   └─ ForwardProviderFactory.java       # 转发实现装配
 │   │       ├─ handler/
 │   │       │   ├─ ConnectionEventHandler.java       # 连接建立/断开等生命周期事件
 │   │       │   ├─ DataEventHandler.java             # 业务数据入站处理（鉴权拦截/排重/调用 ParserProvider/ExtProvider/Forward）
 │   │       │   ├─ MdcLogHandler.java                # 将远端 IP/端口写入 MDC
 │   │       │   └─ IdleHandlerFactory.java           # 按配置生成 IdleStateHandler
 │   │       ├─ message/
 │   │       │   ├─ parser/
 │   │       │   │   ├─ MsgParserProvider.java        # 上行解析接口（msgId -> DTO 列表）
 │   │       │   │   ├─ MsgParserProviderFactory.java # 解析实现装配工厂（支持数组形式配置）
 │   │       │   │   ├─ MsgExtParserProvider.java     # 上行扩展接口（对 DTO 做二次解析/补充）
 │   │       │   │   ├─ MsgExtParserProviderFactory.java # 扩展实现装配工厂（单个覆盖）
 │   │       │   │   ├─ provider/                     # 上行消息解析实现（0x0100/0x0102/0x0200/...）
 │   │       │   │   └─ extprovider/                  # 位置等消息的扩展解析实现
 │   │       │   └─ sender/
 │   │       │       ├─ MsgSenderProvider.java        # 下行发送接口
 │   │       │       ├─ DefaultPlatformMsgSenderProvider.java # 默认下行发送（构建 PlatformMessage 并写回 channel）
 │   │       │       └─ MsgSenderProviderFactory.java # 下行发送实现装配工厂
 │   │       ├─ model/
 │   │       │   ├─ TerminalMessage.java              # 终端上行消息模型
 │   │       │   ├─ PlatformMessage.java              # 平台下行消息模型
 │   │       │   └─ dto/                              # 业务 DTO（BaseDTO/LocationDTO/RegisterDTO...）
 │   │       ├─ session/
 │   │       │   ├─ Session.java                      # 会话模型
 │   │       │   ├─ SessionManager.java               # 会话管理（channelId/terminalId 映射）
 │   │       │   ├─ SessionRouter.java                # terminalId -> Channel 路由
 │   │       │   └─ FlowIdGenerator.java              # 下行 flowId(16bit) 生成
 │   │       └─ util/
 │   │           ├─ BcdUtil.java                      # BCD 工具
 │   │           ├─ CodecUtil.java                    # 编解码工具
 │   │           ├─ HexUtil.java                      # Hex 工具
 │   │           └─ ReplyHelper.java                  # 通用应答辅助
 │   └─ resources/
 │       ├─ application.yaml                          # 配置（TCP、排重、provider/extProvider、forward 等）
 │       └─ logback.xml                               # 日志配置（含 MDC 模式）
 └─ test/
     └─ java/                                   # 预留单测目录
```

## 关键设计约定
- 仅 TCP，不支持 UDP。
- 扩展点采用类名（FQCN）直配：parser/extProvider/forward/sender。
- 不做接入限流与幂等（幂等交由转发端）。

## Roadmap（规划中）
- HTTP API（health、下发等）
- Redis / HA（跨节点路由、离线队列等）
- Web UI
