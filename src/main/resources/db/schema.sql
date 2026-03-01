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

-- 怪物表（配置表，对应 Excel 怪物表）
CREATE TABLE IF NOT EXISTS `monster` (
    `id` INT NOT NULL COMMENT '怪物唯一标识符（对应Excel的ID列）',
    `name` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '怪物名称（对应Excel的Name列）',
    `icon` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '怪物图标/精灵图资源名称（如NPC1、NPC2）',
    `attack` INT NOT NULL DEFAULT 0 COMMENT '攻击力（对应Excel的attack列）',
    `defence` INT NOT NULL DEFAULT 0 COMMENT '防御力（对应Excel的defence列）',
    `maxhp` INT NOT NULL DEFAULT 0 COMMENT '最大生命值（对应Excel的maxhp列）',
    `doge` INT NOT NULL DEFAULT 0 COMMENT '闪避值（对应Excel的doge列）',
    `accurate` INT NOT NULL DEFAULT 0 COMMENT '命中值（对应Excel的Accurate列）',
    `crit` INT NOT NULL DEFAULT 0 COMMENT '暴击值（对应Excel的crit列）',
    `doublehit` INT NOT NULL DEFAULT 0 COMMENT '连击值（对应Excel的doublehit列）',
    `reflect` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '反伤值（对应Excel的Reflect列）',
    `item` VARCHAR(512) NOT NULL DEFAULT '' COMMENT '掉落物品配置，格式：道具id_数量范围_万分比，多条用分号分隔',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='怪物表';
