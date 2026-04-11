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

-- 玩家属性表（战斗属性，与 player 通过 player_id 关联，一对一）
CREATE TABLE IF NOT EXISTS `player_attribute` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `player_id` BIGINT NOT NULL COMMENT '玩家ID',
    `hp` INT NOT NULL DEFAULT 100 COMMENT '当前血量',
    `max_hp` INT NOT NULL DEFAULT 100 COMMENT '最大血量',
    `attack` INT NOT NULL DEFAULT 10 COMMENT '攻击力',
    `defence` INT NOT NULL DEFAULT 5 COMMENT '防御力',
    `dodge` INT NOT NULL DEFAULT 0 COMMENT '闪避值',
    `accurate` INT NOT NULL DEFAULT 0 COMMENT '命中值',
    `crit` INT NOT NULL DEFAULT 0 COMMENT '暴击值',
    `doublehit` INT NOT NULL DEFAULT 0 COMMENT '连击值',
    `reflect` INT NOT NULL DEFAULT 0 COMMENT '反伤值',
    `name` VARCHAR(64) NOT NULL DEFAULT '玩家' COMMENT '战斗显示名',
    `icon` VARCHAR(64) NOT NULL DEFAULT 'PLAYER1' COMMENT '战斗头像',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_player_id` (`player_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='玩家属性表';

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

-- 物品表（配置表，装备/道具等）
CREATE TABLE IF NOT EXISTS `item` (
    `id` INT NOT NULL COMMENT '物品唯一标识符',
    `name` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '物品名称',
    `icon` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '图标资源标识',
    `type` INT NOT NULL DEFAULT 0 COMMENT '物品类型：1装备 2宝石 3钥匙 4血瓶',
    `sub_type` INT NOT NULL DEFAULT 0 COMMENT '物品子类型',
    `shape_type` TINYINT NOT NULL DEFAULT 1 COMMENT '背包占格形态 1～9',
    `max_stack` INT NOT NULL DEFAULT 1 COMMENT '最大叠加数量',
    `attack` INT NOT NULL DEFAULT 0 COMMENT '攻击力',
    `defence` INT NOT NULL DEFAULT 0 COMMENT '防御力',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物品表';

-- 背包解锁次序表（配置表，10×7 每格一序号 1～70）
CREATE TABLE IF NOT EXISTS `backpack_unlock_order` (
    `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `slot_index` TINYINT NOT NULL DEFAULT 0 COMMENT '背包位 0～4',
    `order_number` INT NOT NULL COMMENT '解锁序号 1～70',
    `grid_row` SMALLINT NOT NULL COMMENT '行 0～9',
    `grid_col` SMALLINT NOT NULL COMMENT '列 0～6',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_slot_order` (`slot_index`, `order_number`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='背包解锁次序表';

-- 玩家背包位状态表
CREATE TABLE IF NOT EXISTS `player_backpack_slot` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `player_id` BIGINT NOT NULL COMMENT '玩家ID',
    `slot_index` TINYINT NOT NULL COMMENT '背包位 0～4',
    `max_unlocked_order` INT NOT NULL DEFAULT 16 COMMENT '当前已解锁到的最大序号 1～70，16=4×4已激活',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_player_slot` (`player_id`, `slot_index`),
    KEY `idx_player_id` (`player_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='玩家背包位状态表';

-- 玩家背包放置表（每个占格放置一条）
CREATE TABLE IF NOT EXISTS `player_backpack_item` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `player_id` BIGINT NOT NULL COMMENT '玩家ID',
    `slot_index` TINYINT NOT NULL COMMENT '背包位 0～4',
    `grid_row` SMALLINT NOT NULL COMMENT '占格左上角行',
    `grid_col` SMALLINT NOT NULL COMMENT '占格左上角列',
    `item_id` INT NOT NULL COMMENT '物品配置ID',
    `count` INT NOT NULL DEFAULT 1 COMMENT '数量',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_player_slot` (`player_id`, `slot_index`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='玩家背包放置表';

-- 宝箱表（配置表）
CREATE TABLE IF NOT EXISTS `chest` (
    `id` INT NOT NULL COMMENT '宝箱唯一标识符',
    `name` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '宝箱名称',
    `icon` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '图标资源名，可选',
    `rewards` VARCHAR(512) NOT NULL DEFAULT '' COMMENT '奖励字符串，格式：道具id_数量随机范围_掉落率（万分比），如 1_1-1_10000;2_1-2_5000',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='宝箱表';

-- 大章节表（bigMaps 章节）
CREATE TABLE IF NOT EXISTS `big_map` (
    `id` INT NOT NULL COMMENT '大章节ID（与前端 bigMap.id 一致）',
    `name` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '章节名称，如第一章',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='大章节表';

-- 大章节层表（每章的 layers 项，options 存 JSON 数组）
CREATE TABLE IF NOT EXISTS `big_map_layer` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `big_map_id` INT NOT NULL COMMENT '所属大章节ID',
    `sort_order` INT NOT NULL COMMENT '层顺序，对应 layers 数组下标',
    `options` JSON NOT NULL COMMENT '该层选项数组，如 [1001, 1002, 1003]',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_big_map_id` (`big_map_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='大章节层表';
