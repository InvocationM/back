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
    
    // 移动意图：前端发点击格子，后端返回路径或交互结果
    public static final int MOVE_INTENT = 2002;
    
    // 战斗相关
    public static final int BATTLE_START = 3001;
    public static final int BATTLE_RESULT = 3003;
    
    // 道具相关
    public static final int ITEM_PICKUP = 4001;
    public static final int ITEM_USE = 4002;
    public static final int OPEN_CHEST = 4003;
    
    /** 站在出口格进入大章节下一层小地图（需先 HTTP 缓存下一层地图） */
    public static final int BIG_MAP_USE_EXIT = 5010;
}
