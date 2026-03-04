# 玩家移动 — 意图协议 MOVE_INTENT（方案 A）

**原则**：前端寻路与动画自主；2002 只传「可走到的终点」（遇怪/宝箱时传相邻格），后端只校验移动合法性。事件（怪物/宝箱）由前端走到相邻格后根据本地地图检测，主动发 3001（战斗）或开箱请求，后端校验位置与事件后执行并回结果。

## 协议

- **C→S**：`{ "type": 2002, "targetX": number, "targetY": number, "mapId": number }`（mapId 可选，首次建议带）
- **S→C 成功**：`{ "type": 2002, "code": 200, "status": "SUCCESS", "finalX", "finalY", "events": [], "seqId"? }`
- **S→C 失败**：`{ "type": 2002, "code": 400, "message": "string", "correctPos": { "x", "y" }?, "seqId"? }`

2002 仅做「移动到 target 格」的校验，只返回成功（含最终坐标）或失败（含 correctPos）；不再返回 INTERRUPTED 或 action battle/chest。事件由前端在到达相邻格后主动发 3001（或后续开箱接口），后端只做位置与事件存在性校验。

## 后端

- **MoveIntentProcessor**：收 targetX/targetY；无位置时用 findEntrance 设当前格；**MapPathService** 寻路到 target（目标为事件格时路径只到相邻格），更新 Session 到路径终点，只回 SUCCESS 或 400。
- **MapPathService**：A* 寻路，目标为互动格时路径终点仅为相邻可走格，不包含事件格本身。
- **MapWalkableService**：isWalkable、findEntrance、getCellEvent、getMapSize。

## 前端

- 寻路仅到相邻格（不进入事件格），发送 2002 的 target 为路径终点（可走格）；收到 SUCCESS 后播动画到 finalX/finalY。
- 路径走完后，若意图格为怪物/宝箱且与当前格相邻，由前端主动发 3001（战斗）或开箱请求；后端校验并回结果。

## 历史

- 原「按步 PLAYER_MOVE(2001)」已废弃，由 MOVE_INTENT(2002) 替代；PlayerMoveProcessor 已删除。
- 方案 A：2002 不再返回 INTERRUPTED/action；战斗、开箱由前端到相邻格后触发，后端只校验。
