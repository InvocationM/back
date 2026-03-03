package com.tower.game.common.constant;

/**
 * 消息类型常量
 */
public class MessageType {
    // 心跳消息
    public static final int HEARTBEAT = 1000;
    
    // 登录相关
    public static final int LOGIN = 1001;
    public static final int LOGOUT = 1002;
    
    // 玩家移动（单步，已由 MOVE_INTENT 替代）
    public static final int PLAYER_MOVE = 2001;
    // 移动意图：前端发点击格子，后端返回路径或交互结果
    public static final int MOVE_INTENT = 2002;
    
    // 战斗相关
    public static final int BATTLE_START = 3001;
    public static final int BATTLE_ATTACK = 3002;
    public static final int BATTLE_RESULT = 3003;
    
    // 道具相关
    public static final int ITEM_PICKUP = 4001;
    public static final int ITEM_USE = 4002;
    
    // 场景相关
    public static final int ENTER_FLOOR = 5001;
    public static final int EXIT_FLOOR = 5002;
}
