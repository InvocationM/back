# 玩家移动 — 意图协议 MOVE_INTENT（前端只播动画）

**原则**：前端不拥有位置，只拥有动画。前端仅发「点了哪个格子」，后端返回「能不能走 / 走哪 / 交互结果」，前端只缓存结果并播动画。

## 协议

- **C→S**：`{ "type": 2002, "targetX": number, "targetY": number, "mapId": number }`（mapId 可选，首次建议带）
- **S→C 成功-移动**：`{ "type": 2002, "code": 200, "action": "move", "fromX", "fromY", "path": [{"x", "y"}, ...] }`
- **S→C 成功-战斗**：`{ "type": 2002, "code": 200, "action": "battle", "monsterId", "cellX", "cellY" }`
- **S→C 成功-宝箱**：`{ "type": 2002, "code": 200, "action": "chest", "chestId", "cellX", "cellY" }`
- **S→C 失败**：`{ "type": 2002, "code": 400, "message": "string" }`

## 后端

- **MoveIntentProcessor**：收 targetX/targetY；无位置时用 findEntrance 设当前格；相邻怪物/宝箱返回 action battle/chest；否则 **MapPathService** 寻路，返回 path 并更新 Session 到路径终点。
- **MapPathService**：A* 寻路，目标为互动格时寻到相邻可通行格。
- **MapWalkableService**：isWalkable、findEntrance、getCellEvent、getMapSize。

## 前端

- **MapController**：点击只发 MOVE_INTENT(targetX, targetY)；收到 200 后按 action 分支：move → 缓存 path 播动画，battle → OnRequestBattle，chest → OpenChestAt。
- 不再使用 Pathfinder、_pendingPath、_waitingForAck；位置与路径仅来自后端响应。

## 历史

- 原「按步 PLAYER_MOVE(2001)」已废弃，由 MOVE_INTENT(2002) 替代；PlayerMoveProcessor 已删除。
