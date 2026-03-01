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

---

## 待办（可选）

- 其他模块（移动、背包、战斗等）按《后端权威_Unity仅展示_改动点.md》推进。
