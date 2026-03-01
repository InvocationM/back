-- 玩家表初始化数据（仅当不存在时插入，避免重复执行报错）
INSERT
IGNORE INTO `player` (`id`, `username`, `password`, `nickname`, `level`, `exp`, `current_floor`, `create_time`, `update_time`) VALUES
(1, 'player1', 'e10adc3949ba59abbe56e057f20f883e', '玩家一', 1, 0, 1, NOW(), NOW()),
(2, 'player2', 'e10adc3949ba59abbe56e057f20f883e', '玩家二', 3, 500, 2, NOW(), NOW()),
(3, 'player3', 'e10adc3949ba59abbe56e057f20f883e', '玩家三', 5, 1200, 4, NOW(), NOW());






-- 怪物表插入数据
INSERT INTO `monster` (`id`, `name`, `icon`, `attack`, `defence`, `maxhp`, `doge`, `accurate`, `crit`, `doublehit`,
                       `reflect`, `item`, `create_time`, `update_time`)
VALUES (1, '绿泡泡', 'NPC11', 5, 1, 20, 43, 26, 43, 9, 0, '1_1-1_10000;4_1-1_5000;5_1-1_5000;6_1-1_2000;',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       (2, '红泡泡', 'NPC21', 10, 2, 50, 45, 27, 45, 9, 0, '2_1-14_10000;4_1-1_5000;5_1-1_5000;6_1-1_2000;',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       (3, '黑泡泡', 'NPC31', 20, 3, 100, 46, 28, 46, 10, 0, '3_1-1_10000;4_1-1_5000;5_1-1_5000;6_1-1_2000;',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       (4, '小蝙蝠', 'NPC41', 30, 4, 150, 47, 29, 47, 11, 0, '1_1-1_10000;4_1-1_5000;5_1-1_5000;6_1-1_2000;',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       (5, '蓝袍巫师', 'NPC51', 40, 5, 200, 48, 30, 48, 12, 0, '1_1-1_10000;4_1-1_5000;5_1-1_5000;6_1-1_2000;',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       (6, '红袍巫师', 'NPC61', 50, 6, 250, 49, 31, 49, 13, 0, '1_1-1_10000;4_1-1_5000;5_1-1_5000;6_1-1_2000;',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       (7, '铜傀儡', 'NPC71', 60, 7, 300, 50, 32, 50, 14, 0, '1_1-1_10000;4_1-1_5000;5_1-1_5000;6_1-1_2000;',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       (8, '铁傀儡', 'NPC81', 70, 8, 350, 51, 33, 51, 15, 0, '1_1-1_10000;4_1-1_5000;5_1-1_5000;6_1-1_2000;',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       (9, '骷髅怪', 'NPC91', 80, 9, 400, 52, 34, 52, 16, 0, '1_1-1_10000;4_1-1_5000;5_1-1_5000;6_1-1_2000;',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       (10, '骷髅兵', 'NPC101', 90, 10, 450, 53, 35, 53, 17, 0, '1_1-1_10000;4_1-1_5000;5_1-1_5000;6_1-1_2000;',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       (11, '红骷髅', 'NPC111', 100, 11, 500, 54, 36, 54, 18, 0, '1_1-1_10000;4_1-1_5000;5_1-1_5000;6_1-1_2000;',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       (12, '大蝙蝠', 'NPC121', 110, 12, 550, 55, 37, 55, 19, 0, '1_1-1_10000;4_1-1_5000;5_1-1_5000;6_1-1_2000;',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       (13, '木乃伊', 'NPC131', 120, 13, 600, 56, 38, 56, 20, 0, '1_1-1_10000;4_1-1_5000;5_1-1_5000;6_1-1_2000;',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       (14, '木乃伊卫士', 'NPC141', 130, 14, 650, 57, 39, 57, 21, 0, '1_1-1_10000;4_1-1_5000;5_1-1_5000;6_1-1_2000;',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       (15, '粘土怪', 'NPC151', 140, 15, 700, 58, 40, 58, 22, 0, '1_1-1_10000;4_1-1_5000;5_1-1_5000;6_1-1_2000;',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       (16, '红色武士', 'NPC161', 150, 16, 750, 59, 41, 59, 23, 0, '1_1-1_10000;4_1-1_5000;5_1-1_5000;6_1-1_2000;',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       (17, '蓝色武士', 'NPC171', 160, 17, 800, 60, 42, 60, 24, 0, '1_1-1_10000;4_1-1_5000;5_1-1_5000;6_1-1_2000;',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       (18, '小魔王', 'NPC181', 170, 18, 850, 61, 43, 61, 25, 0, '1_1-1_10000;4_1-1_5000;5_1-1_5000;6_1-1_2000;',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       (19, '黄卫兵', 'NPC191', 180, 19, 900, 62, 44, 62, 26, 0, '1_1-1_10000;4_1-1_5000;5_1-1_5000;6_1-1_2000;',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       (20, '红卫兵', 'NPC201', 190, 20, 950, 63, 45, 63, 27, 0, '1_1-1_10000;4_1-1_5000;5_1-1_5000;6_1-1_2000;',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       (21, '杀手', 'NPC211', 200, 21, 1000, 64, 46, 64, 28, 0, '1_1-1_10000;4_1-1_5000;5_1-1_5000;6_1-1_2000;',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       (22, '蓝卫兵', 'NPC221', 210, 22, 1050, 65, 47, 65, 29, 0, '1_1-1_10000;4_1-1_5000;5_1-1_5000;6_1-1_2000;',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       (23, '骷髅卫兵', 'NPC231', 220, 23, 1100, 66, 48, 66, 30, 0, '1_1-1_10000;4_1-1_5000;5_1-1_5000;6_1-1_2000;',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       (24, '粘液人', 'NPC241', 230, 24, 1150, 67, 49, 67, 31, 0, '1_1-1_10000;4_1-1_5000;5_1-1_5000;6_1-1_2000;',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       (25, '高阶巫师', 'NPC251', 240, 25, 1200, 68, 50, 68, 32, 0, '1_1-1_10000;4_1-1_5000;5_1-1_5000;6_1-1_2000;',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       (26, '红蝙蝠', 'NPC261', 250, 26, 1250, 69, 51, 69, 33, 0, '1_1-1_10000;4_1-1_5000;5_1-1_5000;6_1-1_2000;',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       (27, '泡泡王', 'NPC271', 260, 27, 1300, 70, 52, 70, 34, 0, '1_1-1_10000;4_1-1_5000;5_1-1_5000;6_1-1_2000;',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       (28, '红女巫', 'NPC281', 270, 28, 1350, 71, 53, 71, 35, 0, '1_1-1_10000;4_1-1_5000;5_1-1_5000;6_1-1_2000;',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       (29, '黄女巫', 'NPC291', 280, 29, 1400, 72, 54, 72, 36, 0, '1_1-1_10000;4_1-1_5000;5_1-1_5000;6_1-1_2000;',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       (30, '高阶卫兵', 'NPC301', 290, 30, 1450, 73, 55, 73, 37, 0, '1_1-1_10000;4_1-1_5000;5_1-1_5000;6_1-1_2000;',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       (31, '卫兵队长', 'NPC311', 300, 31, 1500, 74, 56, 74, 38, 0, '1_1-1_10000;4_1-1_5000;5_1-1_5000;6_1-1_2000;',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       (32, '魔王', 'NPC321', 310, 32, 1550, 75, 57, 75, 39, 0, '1_1-1_10000;4_1-1_5000;5_1-1_5000;6_1-1_2000;',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       (33, '黑暗巫师', 'NPC331', 320, 33, 1600, 76, 58, 76, 40, 0, '1_1-1_10000;4_1-1_5000;5_1-1_5000;6_1-1_2000;',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);



-- 物品表插入数据
INSERT INTO `item` (`id`, `name`, `icon`, `type`, `sub_type`, `attack`, `defence`, `create_time`, `update_time`)
VALUES (1, '测试装备', '0', 1, 0, 5, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       (2, '测试宝石', '1', 2, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       (3, '测试钥匙', '2', 3, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);


-- 宝箱表插入数据
INSERT INTO `chest` (`id`, `name`, `icon`, `rewards`, `create_time`, `update_time`) VALUES
    (1, '普通宝箱', '', '1_1-1_10000;2_1-2_5000', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);



