package com.game.shooter.android;

import android.os.Bundle;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.game.shooter.ShooterGame;

/**
 * Android 启动入口，由 AndroidManifest.xml 中配置的 Activity 调用
 */
public class AndroidLauncher extends AndroidApplication {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
        config.useImmersiveMode = true;  // 全屏沉浸模式
        config.useWakelock = true;       // 游戏时屏幕常亮
        initialize(new ShooterGame(), config);
    }
}
