# 待办：用户上下文与 WebSocket 创建时机

> 来源：用户上下文与WebSocket创建时机分析  
> 状态：待实现

---

## 一、当前实现梳理

- **WebSocket 连接**：客户端连 `WebSocketConfig` 注册路径 **`/game`**；登录接口返回的 `websocketUrl` 为 **`/ws`**，**需统一**。
- **用户上下文（PlayerSession）**：在 `GameWebSocketHandler.afterConnectionEstablished` 里**立即**用硬编码测试用户创建会话，**无 token 校验**。
- **消息类型**：已定义 `MessageType.LOGIN(1001)` / `LOGOUT(1002)`，但无对应 Processor；LOGIN/LOGOUT 未做绑定用户逻辑。

结论：当前是「连接即创建用户上下文」，且未与登录 token 关联。

---

## 二、WebSocket 连接应在何时建立？

**建议：在 HTTP 登录成功之后建立。**

- 流程：先 `POST /api/auth/login` → 拿到 `token` 和 `websocketUrl` → 再带 token 建立 WebSocket。
- 保证只有已登录用户发起游戏 WS 连接，服务端可用 token 校验身份。

---

## 三、用户上下文应在何时创建？

| 维度 | 方案 A：连接时创建（带 token 鉴权） | 方案 B：首条 LOGIN 消息时创建 |
|------|-------------------------------------|-------------------------------|
| **时机** | `afterConnectionEstablished` 中取 token，校验通过后 `createSession` | 收到第一条 `type=1001` 且带 token，校验通过后 `createSession` |
| **未认证状态** | 无：连接时就必须带有效 token | 有：「已连接未登录」，仅允许 1001 |
| **与现有协议** | 需约定 URL 或首包带 token（如 `?token=xxx`） | 与现有 `MessageType.LOGIN` 一致 |
| **实现复杂度** | Handler 内解析 query/header + 鉴权即可 | 需区分「无 session 只允许 1001」与「有 session 正常分发」 |

**推荐**：尽快绑定用户、实现简单 → **方案 A**；与 LOGIN 语义一致、支持「先连后鉴权」→ **方案 B**。

---

## 四、推荐整体流程（方案 A：连接时鉴权并创建）

1. **客户端**
   - 先 `POST /api/auth/login`，拿到 `token`、`websocketUrl`（后端需改为与 `/game` 一致）。
   - 再建立 WebSocket 并携带 token：`wss://host/game?token=xxx` 或 subprotocol/自定义 header。

2. **服务端**
   - 在 **`afterConnectionEstablished`** 中：
     - 从 `WebSocketSession` 的 URI query（或 handshake 参数）取 `token`。
     - 调用鉴权服务校验 token，解析 `userId`、`username`。
     - 校验通过：`sessionManager.createSession(userId, username, session)`，发送 welcome。
     - 校验失败：关闭连接或发错误后关闭，**不创建** PlayerSession。

3. **结果**
   - WebSocket 在登录成功后建立且带 token。
   - 用户上下文在**连接建立且 token 校验通过**时创建。

**若采用方案 B**：`afterConnectionEstablished` 不调用 `createSession`；`handleTextMessage` 中无 session 时只允许 `type==1001`（LOGIN）且 payload 带 token，校验通过后 `createSession`；需新增 **LoginProcessor** 处理 1001。

---

## 五、落地要点（实现时对照）

- [ ] **统一 WebSocket 路径**：登录接口返回的 `websocketUrl` 与实际注册路径一致（如 `ws://{host}:{port}/game`，或统一为 `/ws` 并改 `WebSocketConfig`）。
- [ ] **移除硬编码测试用户**：不再用 `TEST_USER_ID`/`TEST_USERNAME`；改为基于 token 解析的真实用户，或方案 B 在 LOGIN 时创建。
- [ ] **鉴权服务**：提供可复用的「token → userId, username」校验（与 HTTP 登录共用），供方案 A 的 Handler 或方案 B 的 LoginProcessor 调用。
- [ ] **断开与清理**：保持 `afterConnectionClosed` / `handleTransportError` 中 `sessionManager.removeSession(session)`、`executorCache.invalidate(session.getId())`。

---

实现时按上述时机建立连接并在「鉴权通过」时创建用户上下文即可。
