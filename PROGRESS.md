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

---

## 待办（可选）

- 其他模块（移动、背包、战斗等）按《后端权威_Unity仅展示_改动点.md》推进。
- 移动第二版：持久化、ENTER_FLOOR、限频、重连同步等见计划文档。
