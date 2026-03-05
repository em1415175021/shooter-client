package com.game.shooter.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.game.shooter.ShooterGame;
import com.game.shooter.network.SocketManager;
import org.json.JSONObject;

/**
 * 等待大厅界面
 *
 * 显示：
 *   - 房间码（让房主发给朋友）
 *   - 当前等待玩家数（x/3）
 *   - 3人满后自动倒计时进入游戏
 *
 * 无需 Scene2D，直接用 ShapeRenderer + SpriteBatch 绘制
 */
public class LobbyScreen extends ScreenAdapter {

    private final ShooterGame game;
    private final String      roomCode;
    private final String      myPlayerId;
    private final int[][]     tileMap;
    private final boolean     isHost;     // 是否是房主

    private int   playerCount  = 1;       // 当前房间人数
    private float countdownTimer = -1f;   // 倒计时（-1=未开始）
    private boolean gameStarting = false;

    private BitmapFont   font;
    private BitmapFont   bigFont;
    private ShapeRenderer shape;

    // 动画用（点点点等待动画）
    private float dotTimer = 0f;
    private int   dotCount = 0;

    public LobbyScreen(ShooterGame game, String roomCode, String myPlayerId,
                       int[][] tileMap, boolean isHost) {
        this.game       = game;
        this.roomCode   = roomCode;
        this.myPlayerId = myPlayerId;
        this.tileMap    = tileMap;
        this.isHost     = isHost;

        font    = new BitmapFont();
        font.getData().setScale(1.8f);

        bigFont = new BitmapFont();
        bigFont.getData().setScale(3.5f);

        shape = new ShapeRenderer();

        // 注册房间事件监听（等待阶段）
        game.socketManager.setGameStateListener(null); // 清除旧监听
        setupRoomListener();
    }

    private void setupRoomListener() {
        // socket 已在 MenuScreen 里完成连接，这里只更新监听器即可
        // 注意：不要重新调用 connectAndCreate/connectAndJoin！

        // 注册大厅专属监听（player_joined / game_start / player_left）
        game.socketManager.setLobbyUpdateListener(new SocketManager.LobbyUpdateListener() {
            @Override
            public void onPlayerCountChanged(int count) {
                playerCount = count;
            }

            @Override
            public void onGameStarting() {
                gameStarting   = true;
                countdownTimer = 3f;  // 3秒倒计时后切换到 GameScreen
            }

            @Override
            public void onPlayerLeft(String playerId) {
                if (playerCount > 1) playerCount--;
            }
        });
    }

    @Override
    public void render(float delta) {
        // 清屏 —— 深蓝背景
        Gdx.gl.glClearColor(0.04f, 0.04f, 0.14f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        int w = Gdx.graphics.getWidth();
        int h = Gdx.graphics.getHeight();

        // 倒计时处理
        if (gameStarting && countdownTimer > 0) {
            countdownTimer -= delta;
            if (countdownTimer <= 0) {
                // 切换到游戏界面
                game.setScreen(new GameScreen(game, tileMap, myPlayerId));
                return;
            }
        }

        // 等待动画
        dotTimer += delta;
        if (dotTimer > 0.5f) {
            dotTimer = 0;
            dotCount = (dotCount + 1) % 4;
        }

        game.batch.begin();

        // ── 标题 ──
        drawCenteredText(bigFont, "等待大厅", w, h * 0.82f, new Color(0.4f, 0.9f, 1f, 1f));

        // ── 房间码 ──
        drawCenteredText(font, "房间码：", w, h * 0.68f, new Color(0.8f, 0.8f, 0.8f, 1f));
        drawCenteredText(bigFont, roomCode, w, h * 0.57f, Color.YELLOW);

        // ── 提示 ──
        if (isHost) {
            drawCenteredText(font, "把房间码发给你的朋友！", w, h * 0.46f,
                    new Color(0.6f, 1f, 0.6f, 1f));
        }

        // ── 玩家数 ──
        String dots = ".".repeat(dotCount);
        String waitMsg = gameStarting
                ? "游戏即将开始..."
                : "等待玩家中" + dots;
        drawCenteredText(font, waitMsg, w, h * 0.35f, new Color(0.9f, 0.9f, 0.5f, 1f));

        // ── 人数进度 ──
        drawCenteredText(bigFont, playerCount + " / 3", w, h * 0.24f, Color.WHITE);

        // ── 倒计时 ──
        if (gameStarting && countdownTimer > 0) {
            int sec = (int) Math.ceil(countdownTimer);
            drawCenteredText(bigFont, sec + "秒后开始！", w, h * 0.12f,
                    new Color(1f, 0.5f, 0.2f, 1f));
        } else {
            drawCenteredText(font, "满3人自动开始", w, h * 0.12f,
                    new Color(0.5f, 0.5f, 0.5f, 1f));
        }

        game.batch.end();

        // ── 玩家图标 ──
        drawPlayerIcons(w, h);
    }

    /** 绘制3个玩家槽位的图标 */
    private void drawPlayerIcons(int w, int h) {
        float cy   = h * 0.35f - 60;
        float gap  = 90f;
        float startX = w / 2f - gap;

        shape.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < 3; i++) {
            float cx = startX + i * gap;
            if (i < playerCount) {
                // 已加入 - 亮绿色
                shape.setColor(new Color(0.2f, 0.85f, 0.3f, 1f));
            } else {
                // 空槽 - 暗灰色
                shape.setColor(new Color(0.3f, 0.3f, 0.3f, 0.6f));
            }
            shape.circle(cx, cy, 22);
        }
        shape.end();

        shape.begin(ShapeRenderer.ShapeType.Line);
        shape.setColor(Color.WHITE);
        for (int i = 0; i < 3; i++) {
            shape.circle(startX + i * gap, cy, 22);
        }
        shape.end();
    }

    /** 居中绘制文字 */
    private void drawCenteredText(BitmapFont f, String text, float screenW, float y, Color color) {
        GlyphLayout layout = new GlyphLayout(f, text);
        f.setColor(color);
        f.draw(game.batch, text, (screenW - layout.width) / 2f, y);
    }

    @Override
    public void dispose() {
        font.dispose();
        bigFont.dispose();
        shape.dispose();
    }
}
