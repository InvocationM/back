-- 玩家表
CREATE TABLE IF NOT EXISTS `player` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '玩家ID',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `password` VARCHAR(255) NOT NULL COMMENT '密码（加密后）',
    `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称',
    `level` INT DEFAULT 1 COMMENT '等级',
    `exp` BIGINT DEFAULT 0 COMMENT '经验值',
    `current_floor` INT DEFAULT 1 COMMENT '当前楼层',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_level` (`level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='玩家表';

-- 地图表（单表，整份 JSON 存 data）
CREATE TABLE IF NOT EXISTS `game_map` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `map_id` INT NOT NULL COMMENT '地图ID（业务，即前端 mapId）',
    `data` JSON NOT NULL COMMENT '整份地图JSON，含 mapId、width、height、cells',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_map_id` (`map_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='地图表';
