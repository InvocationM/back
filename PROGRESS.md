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

### 玩家移动 — 意图协议 MOVE_INTENT（方案 A：前端自主移动 + 后端事件验证）

- **原则**：前端寻路与动画自主；2002 只传「可走到的终点」（遇怪/宝箱时传相邻格），后端只校验移动合法性并回 SUCCESS/失败；事件由前端到相邻格后主动发 3001/开箱，后端校验后执行。
- **协议**：见 [docs/玩家移动_意图协议_MOVE_INTENT.md](docs/玩家移动_意图协议_MOVE_INTENT.md)。C→S：`type: 2002, targetX, targetY, mapId`；S→C 仅两种：成功 `status: SUCCESS, finalX, finalY` 或 400 失败（含 correctPos）；不再返回 INTERRUPTED 或 action battle/chest。
- **后端**：`MoveIntentProcessor` 只做「移动到 target」校验，删除遇怪/遇宝箱 INTERRUPTED 逻辑；`MapPathService` 目标为事件格时路径只到相邻格；`BattleStartProcessor` 增加相邻格 + 该格怪物校验，胜利后不更新玩家到怪物格。
- **前端**：寻路只到相邻格，路径走完后根据意图格主动发 3001 或开箱；不再依赖 2002 回包中的 battle/chest。

### 玩家移动前后端分离（第一版，已由 MOVE_INTENT 替代）

- 原 PLAYER_MOVE(2001) 按步校验已废弃，见上条。

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
  - **BattleStartProcessor**（方案 A）：处理 type=3001；校验玩家在 (cellX,cellY) 相邻格、该格确有 monsterId；执行战斗；胜利不更新玩家到怪物格；回包 3003 含 result、cellX、cellY、logs。
- **消息**：C→S `{ "type": 3001, "monsterId", "cellX", "cellY" }`；S→C `{ "type": 3003, "code", "result": { "type", "playerCurrentHp", "totalRounds", "drops" }, "cellX", "cellY", "logs" }`。
- **Unity 端**：需改为发 WS 3001 触发战斗，订阅 3003 用 result.drops + cellX/cellY 做掉落表现，不再本地 ParseAndRoll。

---

## 待办（可选）

- 其他模块（移动、背包、战斗等）按《后端权威_Unity仅展示_改动点.md》推进。
- 移动第二版：持久化、ENTER_FLOOR、限频、重连同步等见计划文档。
