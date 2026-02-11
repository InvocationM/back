# Docker 部署说明

## 文件说明

- `docker-compose.yml` - 仅启动环境服务（MySQL + Redis）
- `docker-compose.full.yml` - 启动完整服务（MySQL + Redis + 应用）
- `Dockerfile` - Spring Boot 应用镜像构建文件
- `application-docker.yml` - Docker 环境配置文件

## 使用方式

### 1. 仅启动环境（开发时使用）

```bash
# 启动 MySQL 和 Redis
docker-compose up -d

# 查看日志
docker-compose logs -f

# 停止服务
docker-compose down
```

### 2. 启动完整服务（包含应用）

```bash
# 构建并启动所有服务
docker-compose -f docker-compose.full.yml up -d --build

# 查看所有服务日志
docker-compose -f docker-compose.full.yml logs -f

# 只查看应用日志
docker-compose -f docker-compose.full.yml logs -f app

# 停止所有服务
docker-compose -f docker-compose.full.yml down

# 停止并删除数据卷（注意：会删除数据库数据）
docker-compose -f docker-compose.full.yml down -v
```

### 3. 重新构建应用

```bash
# 重新构建应用镜像
docker-compose -f docker-compose.full.yml build app

# 重新构建并启动
docker-compose -f docker-compose.full.yml up -d --build app
```

### 4. 查看服务状态

```bash
# 查看运行中的容器
docker-compose -f docker-compose.full.yml ps

# 查看服务健康状态
docker ps
```

## 服务访问地址

- **应用服务**: http://localhost:8080
- **MySQL**: localhost:3306
- **Redis**: localhost:6379

## 数据库配置

- **数据库名**: tower_game
- **用户名**: tower_user
- **密码**: tower_pass
- **Root密码**: root123456

## 注意事项

1. **首次启动**: MySQL 会自动执行 `src/main/resources/db/schema.sql` 初始化数据库
2. **日志文件**: 应用日志会保存在 `./logs` 目录
3. **数据持久化**: MySQL 和 Redis 数据保存在 Docker volumes 中
4. **健康检查**: 应用会等待 MySQL 和 Redis 健康检查通过后才启动
5. **配置文件**: Docker 环境使用 `application-docker.yml`，本地开发使用 `application.yml`

## 故障排查

### 应用无法连接数据库

1. 检查 MySQL 是否正常运行: `docker-compose ps`
2. 检查网络连接: `docker network ls`
3. 查看应用日志: `docker-compose -f docker-compose.full.yml logs app`

### 重新初始化数据库

```bash
# 停止服务并删除数据卷
docker-compose -f docker-compose.full.yml down -v

# 重新启动
docker-compose -f docker-compose.full.yml up -d
```

### 查看容器内部

```bash
# 进入应用容器
docker exec -it tower-game-app sh

# 进入 MySQL 容器
docker exec -it tower-game-mysql bash

# 进入 Redis 容器
docker exec -it tower-game-redis sh
```
