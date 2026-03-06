package com.game.shooter.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.game.shooter.Config;
import com.game.shooter.ShooterGame;
import com.game.shooter.network.SocketManager;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 游戏主界面
 *
 * 布局（横屏）：
 *  ┌─────────────────────────────────────────┐
 *  │  [武器/HP]          [玩家HP条x3]         │
 *  │                                          │
 *  │         ← 全屏地图渲染 →                  │
 *  │                                          │
 *  │  [摇杆]                    [攻击键]       │
 *  └─────────────────────────────────────────┘
 *
 * 控制：
 *   左半屏按住滑动 = 移动摇杆
 *   右下角红圆圈   = 攻击键（持续按住=连续攻击）
 *
 * 武器：
 *   枪  (GUN)   - 远程子弹，面向移动方向射击
 *   刀  (KNIFE) - 近战高伤
 *   盾  (SHIELD)- 近战+减伤35%
 *   徒手(NONE)  - 弱近战
 */
public class GameScreen extends ScreenAdapter implements InputProcessor {

    // ── 玩家颜色方案 ──
    private static final Color[] PLAYER_COLORS = {
            new Color(1f,   0.35f, 0.35f, 1f),   // 红色
            new Color(0.35f,0.75f, 1f,   1f),   // 蓝色
            new Color(0.35f,1f,   0.45f, 1f),   // 绿色
    };

    // ── 地图颜色 ──
    private static final Color COL_GRASS = new Color(0.18f, 0.38f, 0.13f, 1f);
    private static final Color COL_WALL  = new Color(0.42f, 0.42f, 0.44f, 1f);
    private static final Color COL_ROCK  = new Color(0.52f, 0.38f, 0.24f, 1f);

    // ──────────────────────────────────────────
    //  字段
    // ──────────────────────────────────────────

    private final ShooterGame game;
    private final int[][]     tileMap;
    private final String      myPlayerId;

    // 渲染
    private OrthographicCamera worldCam;   // 跟随玩家的世界相机
    private OrthographicCamera hudCam;     // 固定 HUD 相机
    private ShapeRenderer      shape;
    private BitmapFont         font;
    private BitmapFont         bigFont;

    // 游戏状态（由服务器推送更新）
    private final Map<String, PlayerState>  players   = new HashMap<>();
    private final List<BulletState>         bullets   = new ArrayList<>();
    private final List<LootBoxState>        lootBoxes = new ArrayList<>();
    private final Map<String, Integer>      colorMap  = new HashMap<>();
    private int colorCounter = 0;

    // 游戏结束
    private boolean gameOver   = false;
    private String  winnerId   = "";

    // 通知消息（捡到武器时显示）
    private String  noticeMsg  = "";
    private float   noticeTimer = 0f;

    // ── 输入：虚拟摇杆 ──
    private final Vector2 joyCenter = new Vector2();
    private final Vector2 joyKnob   = new Vector2();
    private boolean joyActive      = false;
    private int     joyPointer     = -1;

    // ── 输入：攻击键 ──
    private boolean attackHeld    = false;
    private int     attackPointer = -1;
    private final Vector2 attackBtnPos = new Vector2();  // HUD坐标

    // 当前朝向角（弧度）
    private float facingAngle = 0f;

    // 输入发送节流
    private float inputTimer = 0f;

    // ──────────────────────────────────────────
    //  数据类（服务器状态镜像）
    // ──────────────────────────────────────────

    static class PlayerState {
        String  id;
        float   x, y;
        int     hp;
        String  weapon;
        float   angle;
        boolean alive;
    }

    static class BulletState {
        int   id;
        float x, y;
    }

    static class LootBoxState {
        int     id;
        float   x, y;
        boolean opened;
    }

    // ──────────────────────────────────────────
    //  构造函数
    // ──────────────────────────────────────────

    public GameScreen(ShooterGame game, int[][] tileMap, String myPlayerId) {
        this.game       = game;
        this.tileMap    = tileMap;
        this.myPlayerId = myPlayerId;

        int sw = Gdx.graphics.getWidth();
        int sh = Gdx.graphics.getHeight();

        // 世界相机（初始位置地图中心）
        worldCam = new OrthographicCamera();
        worldCam.setToOrtho(false, sw, sh);
        worldCam.position.set(
                Config.MAP_WIDTH  * Config.TILE_SIZE / 2f,
                Config.MAP_HEIGHT * Config.TILE_SIZE / 2f,
                0);
        worldCam.update();

        // HUD相机（原点=左下角，Y向上）
        hudCam = new OrthographicCamera();
        hudCam.setToOrtho(false, sw, sh);
        hudCam.update();

        shape   = new ShapeRenderer();
        font    = new BitmapFont();
        font.getData().setScale(1.5f);
        bigFont = new BitmapFont();
        bigFont.getData().setScale(3f);

        // 攻击键位置：右下角
        attackBtnPos.set(sw - 130f, 130f);

        // 注册网络监听
        game.socketManager.setLobbyUpdateListener(null); // 清掉大厅监听
        game.socketManager.setGameStateListener(new SocketManager.GameStateListener() {
            @Override
            public void onGameState(JSONObject state) {
                parseState(state);
            }

            @Override
            public void onGameOver(String winner) {
                gameOver = true;
                winnerId = winner;
            }
        });

        // 监听"捡到道具"通知
        // （由 MenuScreen 建立的 roomListener 已经失效，这里只用 gameStateListener）

        Gdx.input.setInputProcessor(this);
    }

    // ──────────────────────────────────────────
    //  解析服务器状态
    // ──────────────────────────────────────────

    private void parseState(JSONObject state) {
        try {
            // 玩家列表
            players.clear();
            JSONArray pa = state.getJSONArray("players");
            for (int i = 0; i < pa.length(); i++) {
                JSONObject p = pa.getJSONObject(i);
                PlayerState ps = new PlayerState();
                ps.id     = p.getString("id");
                ps.x      = (float) p.getDouble("x");
                ps.y      = (float) p.getDouble("y");
                ps.hp     = p.getInt("hp");
                ps.weapon = p.getString("weapon");
                ps.angle  = (float) p.getDouble("angle");
                ps.alive  = p.getBoolean("alive");
                players.put(ps.id, ps);
                if (!colorMap.containsKey(ps.id)) {
                    colorMap.put(ps.id, colorCounter++ % PLAYER_COLORS.length);
                }
            }

            // 子弹列表
            bullets.clear();
            JSONArray ba = state.getJSONArray("bullets");
            for (int i = 0; i < ba.length(); i++) {
                JSONObject b = ba.getJSONObject(i);
                BulletState bs = new BulletState();
                bs.id = b.getInt("id");
                bs.x  = (float) b.getDouble("x");
                bs.y  = (float) b.getDouble("y");
                bullets.add(bs);
            }

            // 箱子列表
            lootBoxes.clear();
            JSONArray la = state.getJSONArray("lootBoxes");
            for (int i = 0; i < la.length(); i++) {
                JSONObject box = la.getJSONObject(i);
                LootBoxState lb = new LootBoxState();
                lb.id     = box.getInt("id");
                lb.x      = (float) box.getDouble("x");
                lb.y      = (float) box.getDouble("y");
                lb.opened = box.getBoolean("opened");
                lootBoxes.add(lb);
            }
        } catch (Exception e) {
            Gdx.app.error("GameScreen", "parseState: " + e.getMessage());
        }
    }

    // ──────────────────────────────────────────
    //  主渲染循环
    // ──────────────────────────────────────────

    @Override
    public void render(float delta) {
        if (gameOver) {
            renderGameOver();
            return;
        }

        // 更新朝向角
        if (joyActive) {
            float dx = joyKnob.x - joyCenter.x;
            float dy = joyKnob.y - joyCenter.y;
            if (dx * dx + dy * dy > 25f) {
                facingAngle = MathUtils.atan2(dy, dx);
            }
        }

        // 更新相机跟随本地玩家
        PlayerState me = players.get(myPlayerId);
        if (me != null && me.alive) {
            float mapW = Config.MAP_WIDTH  * Config.TILE_SIZE;
            float mapH = Config.MAP_HEIGHT * Config.TILE_SIZE;
            float halfW = worldCam.viewportWidth  / 2f;
            float halfH = worldCam.viewportHeight / 2f;
            worldCam.position.x = MathUtils.clamp(me.x, halfW, mapW - halfW);
            worldCam.position.y = MathUtils.clamp(me.y, halfH, mapH - halfH);
        }
        worldCam.update();

        // 发送输入（20Hz节流）
        inputTimer += delta;
        if (inputTimer >= Config.INPUT_SEND_INTERVAL) {
            inputTimer = 0f;
            sendInput();
        }

        // 通知消息计时
        if (noticeTimer > 0) noticeTimer -= delta;

        // ── 清屏 ──
        Gdx.gl.glClearColor(0.1f, 0.13f, 0.1f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // ── 世界渲染 ──
        shape.setProjectionMatrix(worldCam.combined);
        renderMap();
        renderLootBoxes();
        renderPlayers();
        renderBullets();

        // ── HUD渲染 ──
        shape.setProjectionMatrix(hudCam.combined);
        game.batch.setProjectionMatrix(hudCam.combined);
        renderHUD(me);
    }

    // ──────────────────────────────────────────
    //  地图渲染（只渲染视口内的格子）
    // ──────────────────────────────────────────

    private void renderMap() {
        if (tileMap == null) return;
        int ts  = Config.TILE_SIZE;

        // 计算可见格子范围
        int minGX = Math.max(0, (int)((worldCam.position.x - worldCam.viewportWidth  / 2f) / ts) - 1);
        int maxGX = Math.min(Config.MAP_WIDTH  - 1, (int)((worldCam.position.x + worldCam.viewportWidth  / 2f) / ts) + 1);
        int minGY = Math.max(0, (int)((worldCam.position.y - worldCam.viewportHeight / 2f) / ts) - 1);
        int maxGY = Math.min(Config.MAP_HEIGHT - 1, (int)((worldCam.position.y + worldCam.viewportHeight / 2f) / ts) + 1);

        shape.begin(ShapeRenderer.ShapeType.Filled);
        for (int gy = minGY; gy <= maxGY; gy++) {
            for (int gx = minGX; gx <= maxGX; gx++) {
                int tile = tileMap[gy][gx];
                switch (tile) {
                    case Config.TILE_WALL: shape.setColor(COL_WALL);  break;
                    case Config.TILE_ROCK: shape.setColor(COL_ROCK);  break;
                    default:               shape.setColor(COL_GRASS); break;
                }
                shape.rect(gx * ts, gy * ts, ts, ts);
            }
        }
        shape.end();

        // 墙的轮廓线（让墙体更明显）
        shape.begin(ShapeRenderer.ShapeType.Line);
        shape.setColor(0f, 0f, 0f, 0.25f);
        for (int gy = minGY; gy <= maxGY; gy++) {
            for (int gx = minGX; gx <= maxGX; gx++) {
                if (tileMap[gy][gx] != Config.TILE_EMPTY) {
                    shape.rect(gx * ts, gy * ts, ts, ts);
                }
            }
        }
        shape.end();
    }

    // ──────────────────────────────────────────
    //  箱子渲染
    // ──────────────────────────────────────────

    private void renderLootBoxes() {
        shape.begin(ShapeRenderer.ShapeType.Filled);
        for (LootBoxState box : lootBoxes) {
            if (box.opened) {
                // 已开：暗灰色半透明
                shape.setColor(0.35f, 0.35f, 0.35f, 0.4f);
                shape.rect(box.x - 12, box.y - 12, 24, 24);
            } else {
                // 未开：金色箱子
                shape.setColor(0.95f, 0.75f, 0.1f, 1f);
                shape.rect(box.x - 15, box.y - 15, 30, 30);
                // 内部高光
                shape.setColor(1f, 0.95f, 0.5f, 0.8f);
                shape.rect(box.x - 10, box.y - 2, 20, 6);
            }
        }
        shape.end();

        shape.begin(ShapeRenderer.ShapeType.Line);
        for (LootBoxState box : lootBoxes) {
            if (!box.opened) {
                shape.setColor(0.7f, 0.5f, 0f, 1f);
                shape.rect(box.x - 15, box.y - 15, 30, 30);
                // 箱子中间横线
                shape.line(box.x - 15, box.y, box.x + 15, box.y);
            }
        }
        shape.end();
    }

    // ──────────────────────────────────────────
    //  玩家渲染
    // ──────────────────────────────────────────

    private void renderPlayers() {
        float r = Config.PLAYER_RADIUS;

        // ── 玩家本体 ──
        shape.begin(ShapeRenderer.ShapeType.Filled);
        for (PlayerState p : players.values()) {
            if (!p.alive) continue;
            Color col = getPlayerColor(p.id);
            shape.setColor(col);
            shape.circle(p.x, p.y, r);

            // 盾牌视觉：在玩家前方绘制半圆弧（用多边形近似）
            if ("SHIELD".equals(p.weapon)) {
                shape.setColor(0.5f, 0.6f, 1f, 0.7f);
                float arcCX = p.x + MathUtils.cos(p.angle) * (r + 5);
                float arcCY = p.y + MathUtils.sin(p.angle) * (r + 5);
                shape.circle(arcCX, arcCY, 8);
            }

            // 方向指示点
            shape.setColor(Color.WHITE);
            float dotX = p.x + MathUtils.cos(p.angle) * (r * 0.65f);
            float dotY = p.y + MathUtils.sin(p.angle) * (r * 0.65f);
            shape.circle(dotX, dotY, 4.5f);
        }
        shape.end();

        // ── 本地玩家外圈轮廓 ──
        shape.begin(ShapeRenderer.ShapeType.Line);
        PlayerState me = players.get(myPlayerId);
        if (me != null && me.alive) {
            shape.setColor(Color.WHITE);
            shape.circle(me.x, me.y, r + 3f);
        }
        // 死亡玩家画个×
        for (PlayerState p : players.values()) {
            if (p.alive) continue;
            shape.setColor(0.5f, 0.5f, 0.5f, 0.6f);
            float d = r * 0.7f;
            shape.line(p.x - d, p.y - d, p.x + d, p.y + d);
            shape.line(p.x - d, p.y + d, p.x + d, p.y - d);
        }
        shape.end();

        // ── HP 条（玩家头顶） ──
        float barW = r * 2.5f;
        float barH = 5f;
        shape.begin(ShapeRenderer.ShapeType.Filled);
        for (PlayerState p : players.values()) {
            if (!p.alive) continue;
            float bx = p.x - barW / 2f;
            float by = p.y + r + 6f;

            // 背景
            shape.setColor(0.2f, 0.2f, 0.2f, 0.8f);
            shape.rect(bx, by, barW, barH);

            // HP
            float ratio = Math.max(0, p.hp / (float) Config.PLAYER_HP);
            Color hpCol = ratio > 0.5f ? Color.GREEN : (ratio > 0.25f ? Color.YELLOW : Color.RED);
            shape.setColor(hpCol);
            shape.rect(bx, by, barW * ratio, barH);
        }
        shape.end();
    }

    // ──────────────────────────────────────────
    //  子弹渲染
    // ──────────────────────────────────────────

    private void renderBullets() {
        if (bullets.isEmpty()) return;
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(1f, 0.95f, 0.3f, 1f);
        for (BulletState b : bullets) {
            shape.circle(b.x, b.y, 5f);
        }
        shape.end();
        shape.begin(ShapeRenderer.ShapeType.Line);
        shape.setColor(1f, 0.6f, 0f, 0.8f);
        for (BulletState b : bullets) {
            shape.circle(b.x, b.y, 5f);
        }
        shape.end();
    }

    // ──────────────────────────────────────────
    //  HUD 渲染（摇杆、攻击键、HP、武器信息）
    // ──────────────────────────────────────────

    private void renderHUD(PlayerState me) {
        int sw = Gdx.graphics.getWidth();
        int sh = Gdx.graphics.getHeight();

        shape.begin(ShapeRenderer.ShapeType.Filled);

        // ── 虚拟摇杆 ──
        if (joyActive) {
            // 外圈
            shape.setColor(0.5f, 0.5f, 0.5f, 0.35f);
            shape.circle(joyCenter.x, joyCenter.y, Config.JOYSTICK_RADIUS);
            // 滑块
            shape.setColor(0.85f, 0.85f, 0.85f, 0.75f);
            shape.circle(joyKnob.x, joyKnob.y, Config.JOYSTICK_KNOB_R);
        } else {
            // 提示区域（暗显）
            shape.setColor(0.3f, 0.3f, 0.3f, 0.18f);
            shape.circle(120, 120, Config.JOYSTICK_RADIUS);
        }

        // ── 攻击按钮 ──
        if (attackHeld) {
            shape.setColor(1f, 0.25f, 0.25f, 0.92f);
        } else {
            shape.setColor(0.85f, 0.1f, 0.1f, 0.72f);
        }
        shape.circle(attackBtnPos.x, attackBtnPos.y, Config.ATTACK_BTN_RADIUS);

        shape.end();

        // 攻击键轮廓
        shape.begin(ShapeRenderer.ShapeType.Line);
        shape.setColor(Color.WHITE);
        shape.circle(attackBtnPos.x, attackBtnPos.y, Config.ATTACK_BTN_RADIUS);
        shape.end();

        // ── 文字 HUD ──
        game.batch.begin();

        // 武器名 & 本地玩家HP
        if (me != null) {
            String weaponName = weaponDisplayName(me.weapon);
            font.setColor(Color.WHITE);
            font.draw(game.batch, weaponName, 12, sh - 12);
            font.draw(game.batch, "HP: " + me.hp, 12, sh - 38);
        }

        // 攻击键中央文字
        font.setColor(Color.WHITE);
        font.draw(game.batch, "攻击", attackBtnPos.x - 22, attackBtnPos.y + 10);

        // 摇杆提示文字
        if (!joyActive) {
            font.setColor(0.6f, 0.6f, 0.6f, 0.7f);
            font.draw(game.batch, "滑动移动", 60, 50);
        }

        // 通知消息（捡到武器）
        if (noticeTimer > 0 && !noticeMsg.isEmpty()) {
            GlyphLayout layout = new GlyphLayout(font, noticeMsg);
            font.setColor(Color.YELLOW);
            font.draw(game.batch, noticeMsg, (sw - layout.width) / 2f, sh - 65);
        }

        // 所有玩家HP（右上角列表）
        float listY = sh - 15;
        int idx = 0;
        for (PlayerState p : players.values()) {
            Color col = getPlayerColor(p.id);
            String tag = p.id.equals(myPlayerId) ? "我" : ("P" + (idx + 1));
            String info = "[" + tag + "] " + (p.alive ? "HP:" + p.hp : "死亡");
            font.setColor(p.alive ? col : new Color(0.5f, 0.5f, 0.5f, 1f));
            font.draw(game.batch, info, sw - 180, listY);
            listY -= 28;
            idx++;
        }

        game.batch.end();
    }

    // ──────────────────────────────────────────
    //  游戏结束界面
    // ──────────────────────────────────────────

    private void renderGameOver() {
        int sw = Gdx.graphics.getWidth();
        int sh = Gdx.graphics.getHeight();

        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        game.batch.setProjectionMatrix(hudCam.combined);
        game.batch.begin();

        // 主标题
        String headline;
        if (winnerId.equals(myPlayerId)) {
            headline = "你赢了！";
            bigFont.setColor(Color.YELLOW);
        } else if (winnerId.isEmpty()) {
            headline = "平局";
            bigFont.setColor(Color.CYAN);
        } else {
            headline = "你输了...";
            bigFont.setColor(new Color(0.8f, 0.3f, 0.3f, 1f));
        }

        GlyphLayout gl = new GlyphLayout(bigFont, headline);
        bigFont.draw(game.batch, headline, (sw - gl.width) / 2f, sh / 2f + 80);

        // 副标题
        font.setColor(Color.LIGHT_GRAY);
        GlyphLayout gl2 = new GlyphLayout(font, "点击屏幕返回菜单");
        font.draw(game.batch, "点击屏幕返回菜单", (sw - gl2.width) / 2f, sh / 2f - 30);

        game.batch.end();

        // 点击任意处返回
        if (Gdx.input.justTouched()) {
            game.socketManager.disconnect();
            game.setScreen(new MenuScreen(game));
        }
    }

    // ──────────────────────────────────────────
    //  发送输入到服务器
    // ──────────────────────────────────────────

    private void sendInput() {
        float dx = 0f, dy = 0f;
        if (joyActive) {
            dx = joyKnob.x - joyCenter.x;
            dy = joyKnob.y - joyCenter.y;
            float len = (float) Math.sqrt(dx * dx + dy * dy);
            if (len > 5f) { dx /= len; dy /= len; }
            else          { dx = 0; dy = 0; }
        }
        game.socketManager.sendInput(dx, dy, attackHeld, facingAngle);
    }

    // ──────────────────────────────────────────
    //  工具方法
    // ──────────────────────────────────────────

    private Color getPlayerColor(String id) {
        Integer idx = colorMap.get(id);
        return PLAYER_COLORS[idx != null ? idx : 0];
    }

    private String weaponDisplayName(String weapon) {
        switch (weapon) {
            case "GUN":    return "武器: 枪 (远程)";
            case "KNIFE":  return "武器: 刀 (近战)";
            case "SHIELD": return "武器: 盾 (防御)";
            default:       return "武器: 徒手";
        }
    }

    /** libGDX 触摸Y轴翻转（输入Y=0在屏幕顶部，HUD Y=0在底部） */
    private float flipY(int screenY) {
        return Gdx.graphics.getHeight() - screenY;
    }

    // ──────────────────────────────────────────
    //  InputProcessor
    // ──────────────────────────────────────────

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (gameOver) return false;

        float hx = screenX;
        float hy = flipY(screenY);
        int   sw = Gdx.graphics.getWidth();

        // 先检测攻击键（右半屏）
        float distToAtk = new Vector2(hx, hy).dst(attackBtnPos);
        if (distToAtk < Config.ATTACK_BTN_RADIUS + 25 && !attackHeld) {
            attackHeld    = true;
            attackPointer = pointer;
            return true;
        }

        // 摇杆（左半屏）
        if (hx < sw * 0.65f && !joyActive) {
            joyCenter.set(hx, hy);
            joyKnob.set(hx, hy);
            joyActive  = true;
            joyPointer = pointer;
            return true;
        }

        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        if (gameOver) return false;

        float hx = screenX;
        float hy = flipY(screenY);

        if (pointer == joyPointer && joyActive) {
            joyKnob.set(hx, hy);
            // 限制摇杆滑块在外圈内
            Vector2 delta = new Vector2(joyKnob).sub(joyCenter);
            if (delta.len() > Config.JOYSTICK_RADIUS) {
                delta.nor().scl(Config.JOYSTICK_RADIUS);
                joyKnob.set(joyCenter.x + delta.x, joyCenter.y + delta.y);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        if (pointer == joyPointer) {
            joyActive  = false;
            joyPointer = -1;
        }
        if (pointer == attackPointer) {
            attackHeld    = false;
            attackPointer = -1;
        }
        return false;
    }

    // 其余 InputProcessor 方法（不使用）
    @Override public boolean keyDown(int k)           { return false; }
    @Override public boolean keyUp(int k)             { return false; }
    @Override public boolean keyTyped(char c)         { return false; }
    @Override public boolean mouseMoved(int x, int y) { return false; }
    @Override public boolean scrolled(float ax, float ay){ return false; }

    // 新增 touchCancelled 方法（libGDX 1.12.0+ 需要）
    @Override
    public boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    // ──────────────────────────────────────────
    //  生命周期
    // ──────────────────────────────────────────

    @Override
    public void resize(int width, int height) {
        worldCam.viewportWidth  = width;
        worldCam.viewportHeight = height;
        worldCam.update();
        hudCam.setToOrtho(false, width, height);
        hudCam.update();
        attackBtnPos.set(width - 130f, 130f);
    }

    @Override
    public void dispose() {
        shape.dispose();
        font.dispose();
        bigFont.dispose();
    }
}