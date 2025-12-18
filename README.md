# 简洁 JT/T 808 TCP Server（Netty）

 License: Apache-2.0
 
 一个**轻量、无 Spring、无 DB** 的 JT/T 808 TCP 接入层实现，聚焦：

- 上行消息解码、鉴权拦截、解析与转发
- 下行消息编码与下发（通过 `terminalId -> Channel` 路由）
- 解析/扩展/转发组件可插拔（配置类名即可替换）

适合作为：

- 设备接入网关（前置机）
- 你自己的业务平台的“JT808 适配层”基础项目

---

 ## 特性
 
 - **极简依赖，低侵入**
   - 不依赖 Spring / DB / MQ，核心仅保留 Netty + 配置加载 + 必要工具库
   - 更适合作为“接入层/网关”嵌入到你自己的体系里
 - **清晰的职责拆分（pipeline 思维）**
   - 编解码与业务解耦：解码得到 `TerminalMessage`，业务处理聚合在 `DataEventHandler`
   - IO 线程与业务线程池解耦：避免业务阻塞 Netty IO
 - **可插拔、可扩展（配置优先）**
   - 上行解析：`MsgParserProvider`
   - 上行扩展：`MsgExtParserProvider`
   - DTO 转发：`ForwardProvider`
   - 下行发送：`MsgSenderProvider`
   - 通过 `application.yaml` 配置类名（FQCN）即可替换默认实现
 - **会话语义清晰**
   - `SessionManager` 同时维护 `channel` 与 `terminalId` 维度映射
 - **工程化能力内置**
   - 上行鉴权拦截与排重（可配置）
   - 上下行日志与 MDC 上下文（便于排障/追踪）
   - 默认 `ForwardProvider` 仅输出日志，生产环境需要按业务实现转发（MQ/HTTP/DB 等）
 
 ---
 
 ## 快速开始

 ### 1) 环境要求

 - JDK 11+
 - Maven 3.8+

 ### 2) 编译与运行
 
 ```bash
 mvn -q -DskipTests package
 ```
 
 推荐方式：作为基建组件运行/调试（IDE 直接运行入口类）
 
 - 入口类：`cn.jascript.zt808.boot.Application`
 - 说明：本项目更偏“接入层基建”，默认转发仅打印日志，生产使用需实现并配置自己的 `ForwardProvider`
 
 可选（仅用于本地验证/演示）：你也可以运行构建产物（fat jar 位于 `target/*-all.jar`）：
 
 ```bash
 java -jar target/zt808-1.0-SNAPSHOT-all.jar
 ```
 
 默认端口：`6808`（见 `src/main/resources/application.yaml`）。

 ### 3) 本地发送测试报文
 
 下面是一个可用的 hex 报文示例（包含头尾 `7E`）。在 macOS/Linux 可这样发送：

 ```bash
 echo "7E0002000001454075628200CA597E" | xxd -r -p | nc 127.0.0.1 6808
 ```
 
 注：`nc` 发送的是二进制流，`xxd -r -p` 用于把 hex 转为 bytes。

 另外，项目测试目录提供了 `JT808TestKit`（造包工具）可用于本地构造报文做验证/参考；但协议兼容性与边界场景仍建议以真实终端或线上日志数据回放为准。

---

## 测试（基础回归）

项目提供 `PipelineSmokeTest` 作为基础回归（baseline）测试：

- 覆盖 Netty pipeline 关键环节（切帧/反转义/BCC/解码/业务分发/编码）
- 典型链路（注册/鉴权/心跳/位置）应能正常跑通并产生正确回包

如果该测试失败，通常意味着接入层核心链路回归，建议优先检查：

- 编解码（escape/BCC/header/bodyLength）
- `DataEventHandler`（鉴权拦截/排重/回包逻辑）
- `application.yaml` 中 provider 映射与 `replyMode` 配置

运行方式：

```bash
mvn -q test -Dtest=PipelineSmokeTest
```

---

 ## 配置说明（application.yaml）

 主要配置集中在：`src/main/resources/application.yaml`。

 - **server**
   - `server.port`：TCP 监听端口
   - `server.idleSeconds`：空闲断链阈值（设备需周期发送心跳/数据）
   - `server.maxConnections`：最大连接数（预留）
 - **auth**
   - `auth.code`：固定鉴权码（`0x0102` 消息体）
   - `auth.blackList`：终端号黑名单
 - **message.parser.provider**
   - msgId -> `MsgParserProvider`（解析 DTO）
   - 支持 `replyMode`：`general | none | provider`
 - **message.parser.extProvider**
   - msgId -> `MsgExtParserProvider`（对 DTO 二次扩展）
 - **forward.provider**
   - DTO 转发实现（默认 `DefaultForwardProvider` 打日志）
 - **duplicate**
   - 上行排重：`terminalId-msgId-flowId`

---

 ## 消息链路（上行 / 下行）

本章内容已将 `MESSAGE_FLOW.MD` 的关键流程并入 README，便于开源项目“一页读懂”。

 ### 上行链路（Terminal -> Platform）

 ```mermaid
 flowchart TB
 
   subgraph DEV[终端设备]
     DEV1["终端 TCP 连接"]
     DEV2["上行原始帧 0x7E...0x7E"]
   end
 
   subgraph NETTY[Netty Pipeline]
     A["DelimiterBasedFrameDecoder<br/>按 0x7E 切帧"]
     B["EscapeDecoder<br/>反转义 0x7D 0x01/0x02"]
     C["BccValidDecoder<br/>BCC XOR 校验"]
     D["MessageDecoder<br/>解析消息头+体"]
     E["DataEventHandler<br/>业务分发/鉴权拦截/排重"]
   end

   subgraph SESSION[会话与路由]
     SM["SessionManager<br/>会话注册/绑定/鉴权标记"]
   end
 
   subgraph BIZ[业务组件]
     PF["MsgParserProviderFactory<br/>msgId 映射 ParserProvider"]
     EF["MsgExtParserProviderFactory<br/>msgId 映射 ExtProvider"]
     FW["ForwardProviderFactory<br/>转发 DTO"]
   end
 
   DEV1 -->|TCP| NETTY
   DEV2 --> A --> B --> C --> D --> E
 
   E --> PF
   PF -->|"parse 返回 DTO 列表"| E
   E --> EF
   EF -->|"apply 扩展 DTO"| E
   E --> FW
 
   E -->|"注册/鉴权成功后绑定"| SM
 ```

 ### 下行链路（Platform -> Terminal）

 ```mermaid
 flowchart TB
 
   subgraph SRC[命令来源]
     S1[上层服务入口]
   end
 
   subgraph ROUTE[路由与序号]
     R1["SessionRouter<br/>terminalId 映射 Channel"]
     R2["FlowIdGenerator<br/>生成 flowId 16bit"]
   end
 
   subgraph SEND[下发组包/下发]
     P1["DefaultPlatformMsgSenderProvider<br/>组 PlatformMessage"]
     P2["Channel.writeAndFlush<br/>PlatformMessage"]
   end
 
   subgraph ENC[编码与网络]
     E1["EncoderFactory<br/>头+体+BCC+转义+0x7E"]
     E2["TCP 下发到终端"]
   end
 
   S1 --> R1 --> R2 --> P1 --> P2 --> E1 --> E2
 ```

---

 ## 扩展点（可插拔）

 - **上行解析**：实现 `cn.jascript.zt808.message.parser.MsgParserProvider`
 - **上行扩展**：实现 `cn.jascript.zt808.message.parser.MsgExtParserProvider`
 - **下行发送**：实现 `cn.jascript.zt808.message.sender.MsgSenderProvider`
 - **DTO 转发**：实现 `cn.jascript.zt808.forward.ForwardProvider`

通过在 `application.yaml` 中配置实现类全类名（FQCN）完成替换；启动期由 `StartupValidator` 校验必需映射。

---

 ## Roadmap（规划中）
 
以下能力属于“后续需要实现”的功能（TODO），README 会在落地后更新：

 - [ ] **统计 / 可观测**：在线数、连接建立/断开计数
 - [ ] **统计 / 可观测**：上行/下行吞吐量,失败消息量
 - [ ] **HTTP API**：`/health`、下行指令下发等
 - [ ] **Redis / HA**：跨节点会话路由、离线消息队列、流控与粘性策略
 - [ ] **Web UI**：统计数据、设备在线列表、消息下发(等)
 
---