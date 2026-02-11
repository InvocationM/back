-- 玩家表初始化数据（仅当不存在时插入，避免重复执行报错）
INSERT IGNORE INTO `player` (`id`, `username`, `password`, `nickname`, `level`, `exp`, `current_floor`, `create_time`, `update_time`) VALUES
(1, 'player1', 'e10adc3949ba59abbe56e057f20f883e', '玩家一', 1, 0, 1, NOW(), NOW()),
(2, 'player2', 'e10adc3949ba59abbe56e057f20f883e', '玩家二', 3, 500, 2, NOW(), NOW()),
(3, 'player3', 'e10adc3949ba59abbe56e057f20f883e', '玩家三', 5, 1200, 4, NOW(), NOW());
