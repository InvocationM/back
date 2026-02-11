# back

## 环境与 Compose 对应关系

| 环境 | Compose 文件 | Profile | 说明 |
|------|--------------|--------|------|
| 本地只起库 | `docker-compose.yml` | - | 只起 MySQL、Redis，应用在本机跑（如 IDE）时用 |
| 测试全栈 | `docker-compose.full.yml` | test | 一次起 MySQL + Redis + 应用 |
| 测试只起应用 | `docker-compose.test.yml` | test | 仅起应用，需先执行 `docker-compose up -d` 启动 MySQL/Redis |
| 生产 | `docker-compose.prod.yml` | prod | 仅起应用，连接云 MySQL/云 Redis（配置见 `.env` / 环境变量） |
