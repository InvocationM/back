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
    
    // 玩家移动
    public static final int PLAYER_MOVE = 2001;
    
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
