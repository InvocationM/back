# rockTower 后端

Spring Boot 后端目录：`back`。

## 依赖

- Java 17+（以 `pom.xml` 为准）
- Maven
- MySQL
- Redis

## Compose 文件

| 环境 | Compose 文件 | 说明 |
| --- | --- | --- |
| 本地依赖 | `docker-compose.yml` | 只启动 MySQL、Redis，应用在本机 IDE 或命令行运行 |
| 测试全栈 | `docker-compose.full.yml` | MySQL + Redis + 应用 |
| 测试应用 | `docker-compose.test.yml` | 只启动应用，需先启动依赖 |
| 生产 | `docker-compose.prod.yml` | 使用生产配置，连接外部 MySQL/Redis |

## 常用命令

```bash
mvn -q -DskipTests compile
mvn test
```

当前环境若没有 `mvn`，可用 IDE 或 Docker 镜像执行编译。

## 关键模块

| 模块 | 说明 |
| --- | --- |
| `api` | HTTP Controller |
| `server/handler` | WebSocket 入口 |
| `server/processor` | WebSocket 消息处理器 |
| `server/session` | 玩家会话与当前地图状态 |
| `service` | 地图、战斗、背包、章节等业务 |
| `common/dto` | 请求/响应 DTO |
| `resources/db` | 初始化 SQL |

## 当前协议入口

- 地图：`POST /api/map/{mapId}`
- 大章节：`POST /api/big-map/bigMapList`、`POST /api/big-map/start`
- 背包：`POST /api/backpack/getBackpack`、`/move`、`/unlock`、`/pickupMapCell`
- 开门：`POST /api/map/openDoor`
- WebSocket：`MOVE_INTENT(2002)`、`BATTLE_START(3001)`、`ITEM_PICKUP(4001)`、`BIG_MAP_USE_EXIT(5010)`

详细协议见：

- `../文档总览.md`
- `docs/后端说明.md`
- `docs/背包系统.md`
