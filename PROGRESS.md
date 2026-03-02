# 后端进度 / 看板

## 已完成

### 地图格子事件权重服务端化（当前迭代）

- **需求**：将地图每个格子“按权重选一个事件”的逻辑从客户端移到服务端，保证同一地图请求下每个格子只返回一个事件。
- **实现**：
  - 新增 `CellEventEntry` DTO（type、id、weight），与客户端结构一致。
  - 新增 `MapWeightSelector`：对地图 JSON 逐格处理，多事件格按权重随机选一个（权重为 0 不参与，概率 = 事件权重/总权重），将 `events` 改为只含一个元素。
  - `GameMapService.getByMapId`：在返回前调用 `MapWeightSelector.applyWeightSelectionPerCell(map.getData())`，用处理后的 JSON 覆盖 `map.data`。
- **接口**：`POST /api/map/{mapId}` 返回的 `data` 中，每个格子的 `events` 数组至多一个元素。
- **客户端**：Unity MapLoader 不再调用 `SelectEventByWeight`，直接使用服务端下发的唯一事件（`events[0]`），并移除了客户端权重选择相关代码。

### 玩家移动前后端分离（第一版最小闭环）

- **范围说明**：见 [docs/玩家移动前后端分离_第一版范围.md](docs/玩家移动前后端分离_第一版范围.md)。
- **服务端**：`PlayerSession` 增加 mapId、cellX、cellY；新增 `MapWalkableService`（isWalkable、findEntrance）；新增 `PlayerMoveHandler`（PLAYER_MOVE 一步校验，200/400）。首次移动无位置时用地图入口作当前格。
- **客户端**：按步发 PLAYER_MOVE，收到 200 后再播动画；新点击直接替换目标；未连接 WebSocket 时回退为本地移动。
- **第一版不做**：位置落库、ENTER_FLOOR、限频、stepId、失败回包带当前坐标、重连带位置、超时重试。

### 用户上下文轻量重构（SessionState + PlayerSession）

- **目标**：将会话状态与连接解耦，便于后续按需接入 Redis / 断线重连，当前仍为内存存储。
- **实现**：
  - 新增 **SessionState** DTO：可序列化字段 sessionId、userId、username、gameStatus、mapId、cellX、cellY、loginTime、lastActiveTime、hp、maxHp；供内存或后续 Redis 存储。
  - **PlayerSession** 重构：内部持有一份 `SessionState`；对外保留原有 getter/setter（getCellX、setMapId 等）兼容现有 Processor；新增 `getState()`（返回状态快照）、`updateState(Consumer<SessionState>)`。
  - **SessionManager**：保持内存 Map，create/remove 逻辑不变。
  - **Processor**：无需改调用方式，仍通过 session 的 get/set 读写状态。
- **扩展点**：若后续需要多实例或断线重连恢复状态，可增加 SessionRedisRepository，将状态的读写从内存改为 Redis，无需再改 Processor/Handler。

### 战斗逻辑后端化（方案 B + 掉落后端随机）

- **目标**：战斗计算与掉落随机均在后端完成，Unity 仅发 BATTLE_START、收 BATTLE_RESULT 并做表现。
- **实现**：
  - **SessionState**：增加战斗属性 attack、defence、dodge、accurate、crit、doublehit、reflect、name、icon；建会话时用默认值填充（方案 B，无境界表）。
  - **CombatantSnapshot / BattleResultDto / DropItemDto**：`common.dto.battle` 包；战斗入参与结果结构。
  - **MonsterService.getById**：单条怪物实时查库。
  - **BattleEngineService**：命中/伤害/暴击/反伤/连击、50 回合上限，与 Unity BattleEngine 一致；`buildPlayerSnapshot(SessionState)`、`buildMonsterSnapshot(Monster)`、`run(...)`。
  - **DropRollService**：解析 Monster.item 字符串（道具id_数量范围_万分比），按万分比 roll，返回 `List<DropItemDto>`。
  - **BattleStartProcessor**：处理 type=3001（monsterId、cellX、cellY）；从 session 取玩家快照、从 MonsterService 取怪物；执行战斗；胜利时调用 DropRollService 写入 result.drops；回包 3003 含 result、cellX、cellY、logs。
- **消息**：C→S `{ "type": 3001, "monsterId", "cellX", "cellY" }`；S→C `{ "type": 3003, "code", "result": { "type", "playerCurrentHp", "totalRounds", "drops" }, "cellX", "cellY", "logs" }`。
- **Unity 端**：需改为发 WS 3001 触发战斗，订阅 3003 用 result.drops + cellX/cellY 做掉落表现，不再本地 ParseAndRoll。

---

## 待办（可选）

- 其他模块（移动、背包、战斗等）按《后端权威_Unity仅展示_改动点.md》推进。
- 移动第二版：持久化、ENTER_FLOOR、限频、重连同步等见计划文档。
