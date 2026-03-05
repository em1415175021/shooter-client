package com.game.shooter;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.game.shooter.network.SocketManager;
import com.game.shooter.screens.MenuScreen;

/**
 * 游戏主类，持有全局共享资源
 * LibGDX 通过 AndroidLauncher 调用 create() 初始化
 */
public class ShooterGame extends Game {

    // 全局共享渲染资源（所有Screen共用，避免重复创建）
    public SpriteBatch   batch;
    public ShapeRenderer shapeRenderer;

    // 网络管理器（全局单例）
    public SocketManager socketManager;

    @Override
    public void create() {
        batch         = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        socketManager = new SocketManager();

        // 启动时显示菜单界面
        setScreen(new MenuScreen(this));
    }

    @Override
    public void dispose() {
        super.dispose();
        batch.dispose();
        shapeRenderer.dispose();
        socketManager.disconnect();
    }
}
