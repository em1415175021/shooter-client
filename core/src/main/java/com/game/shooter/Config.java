package com.game.shooter;

/**
 * 游戏全局常量配置
 * ⚠️ 部署前请修改 SERVER_URL 为你的 Railway 服务器地址
 */
public class Config {

    // ========== 服务器配置 ==========
    /** Railway 服务器地址，部署后替换为真实地址，例如: https://shooter-game.up.railway.app */
    public static final String DEFAULT_SERVER_URL = "https://your-app.up.railway.app";

    // ========== 地图常量 ==========
    public static final int TILE_SIZE   = 32;   // 每格像素大小（与服务器一致）
    public static final int MAP_WIDTH   = 50;   // 地图列数
    public static final int MAP_HEIGHT  = 30;   // 地图行数

    // TILE类型值
    public static final int TILE_EMPTY  = 0;
    public static final int TILE_WALL   = 1;
    public static final int TILE_ROCK   = 2;

    // ========== 玩家常量 ==========
    public static final float PLAYER_RADIUS = 16f;  // 玩家碰撞圆半径（像素）
    public static final int   PLAYER_HP     = 100;  // 玩家初始血量

    // ========== 输入发送频率 ==========
    public static final float INPUT_SEND_INTERVAL = 0.05f;  // 每50ms发一次输入（20Hz）

    // ========== UI尺寸 ==========
    public static final float JOYSTICK_RADIUS    = 80f;   // 虚拟摇杆外圈半径
    public static final float JOYSTICK_KNOB_R    = 35f;   // 摇杆滑块半径
    public static final float ATTACK_BTN_RADIUS  = 70f;   // 攻击按钮半径
}
