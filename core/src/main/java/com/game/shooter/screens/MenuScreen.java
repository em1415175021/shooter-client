package com.game.shooter.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.game.shooter.Config;
import com.game.shooter.ShooterGame;
import com.game.shooter.network.SocketManager;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * 主菜单界面
 * 玩家可以：
 *   - 输入服务器地址
 *   - 创建房间（获得房间码）
 *   - 输入房间码加入已有房间
 */
public class MenuScreen extends ScreenAdapter {

    private final ShooterGame game;
    private Stage  stage;
    private Skin   skin;

    private TextField serverUrlField;
    private TextField roomCodeField;
    private Label     statusLabel;

    // 用于标题渲染
    private BitmapFont titleFont;

    public MenuScreen(ShooterGame game) {
        this.game = game;
        stage     = new Stage(new ScreenViewport());
        titleFont = new BitmapFont();
        titleFont.getData().setScale(2.5f);

        skin = buildSkin();
        buildUI();

        Gdx.input.setInputProcessor(stage);
    }

    // ─────────────────────────────────────────────
    //  UI 构建
    // ─────────────────────────────────────────────

    /** 用代码创建一个最简单的 Skin，无需外部资源文件 */
    private Skin buildSkin() {
        Skin s = new Skin();

        // 1×1 白色像素纹理（用于按钮背景着色）
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(Color.WHITE);
        pm.fill();
        s.add("white", new Texture(pm));
        pm.dispose();

        BitmapFont font = new BitmapFont();
        font.getData().setScale(1.6f);
        s.add("default-font", font);

        // ---- Label 样式 ----
        Label.LabelStyle lblStyle = new Label.LabelStyle();
        lblStyle.font      = font;
        lblStyle.fontColor = Color.WHITE;
        s.add("default", lblStyle);

        // ---- TextField 样式 ----
        TextField.TextFieldStyle tfStyle = new TextField.TextFieldStyle();
        tfStyle.font            = font;
        tfStyle.fontColor       = Color.WHITE;
        tfStyle.messageFontColor = new Color(0.6f, 0.6f, 0.6f, 1f);
        tfStyle.background      = tintDrawable(s, new Color(0.08f, 0.08f, 0.2f, 0.95f));
        tfStyle.focusedBackground = tintDrawable(s, new Color(0.1f, 0.1f, 0.35f, 1f));
        tfStyle.cursor          = tintDrawable(s, Color.WHITE);
        s.add("default", tfStyle);

        // ---- TextButton 样式（蓝色） ----
        TextButton.TextButtonStyle btnBlue = new TextButton.TextButtonStyle();
        btnBlue.font      = font;
        btnBlue.fontColor = Color.WHITE;
        btnBlue.up        = tintDrawable(s, new Color(0.15f, 0.45f, 0.85f, 1f));
        btnBlue.down      = tintDrawable(s, new Color(0.1f,  0.3f,  0.6f,  1f));
        btnBlue.over      = tintDrawable(s, new Color(0.25f, 0.55f, 0.95f, 1f));
        s.add("default", btnBlue);

        // ---- TextButton 样式（绿色，加入房间） ----
        TextButton.TextButtonStyle btnGreen = new TextButton.TextButtonStyle();
        btnGreen.font      = font;
        btnGreen.fontColor = Color.WHITE;
        btnGreen.up        = tintDrawable(s, new Color(0.1f,  0.55f, 0.15f, 1f));
        btnGreen.down      = tintDrawable(s, new Color(0.05f, 0.35f, 0.1f,  1f));
        btnGreen.over      = tintDrawable(s, new Color(0.15f, 0.65f, 0.2f,  1f));
        s.add("green", btnGreen);

        return s;
    }

    /** 修改点：将 tint 结果强制转换为 TextureRegionDrawable */
    private TextureRegionDrawable tintDrawable(Skin s, Color color) {
        return (TextureRegionDrawable) new TextureRegionDrawable(s.getRegion("white")).tint(color);
    }

    private void buildUI() {
        Table root = new Table();
        root.setFillParent(true);
        root.pad(30);
        stage.addActor(root);

        // ── 标题 ──
        Label title = new Label("  3人射击对战  ", skin);
        title.setFontScale(1.8f);
        title.setColor(new Color(0.4f, 0.9f, 1f, 1f));
        root.add(title).colspan(2).padBottom(10).row();

        Label subtitle = new Label("非局域网实时联机 · 3人吃鸡", skin);
        subtitle.setFontScale(0.9f);
        subtitle.setColor(new Color(0.7f, 0.7f, 0.7f, 1f));
        root.add(subtitle).colspan(2).padBottom(40).row();

        // ── 服务器地址 ──
        root.add(makeLabel("服务器地址:")).right().padRight(12);
        serverUrlField = new TextField(Config.DEFAULT_SERVER_URL, skin);
        serverUrlField.setMessageText("https://your-app.up.railway.app");
        root.add(serverUrlField).width(480).height(55).padBottom(20).row();

        // ── 创建房间 ──
        TextButton createBtn = new TextButton("  ➕  创建房间  ", skin);
        createBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) { onCreateRoom(); }
        });
        root.add(createBtn).colspan(2).width(380).height(65).padBottom(30).row();

        // ── 分隔线标签 ──
        Label orLabel = makeLabel("—— 或者加入朋友的房间 ——");
        orLabel.setColor(new Color(0.6f, 0.6f, 0.6f, 1f));
        root.add(orLabel).colspan(2).padBottom(20).row();

        // ── 房间码输入 ──
        root.add(makeLabel("房 间 码:")).right().padRight(12);
        roomCodeField = new TextField("", skin);
        roomCodeField.setMessageText("输入6位房间码 (如: AB12CD)");
        roomCodeField.setMaxLength(6);
        root.add(roomCodeField).width(480).height(55).padBottom(20).row();

        // ── 加入房间 ──
        TextButton joinBtn = new TextButton("  🚪  加入房间  ", skin, "green");
        joinBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) { onJoinRoom(); }
        });
        root.add(joinBtn).colspan(2).width(380).height(65).padBottom(30).row();

        // ── 状态提示 ──
        statusLabel = new Label("", skin);
        statusLabel.setColor(Color.YELLOW);
        root.add(statusLabel).colspan(2).row();
    }

    private Label makeLabel(String text) {
        return new Label(text, skin);
    }

    // ─────────────────────────────────────────────
    //  事件处理
    // ─────────────────────────────────────────────

    private void onCreateRoom() {
        String url = serverUrlField.getText().trim();
        if (url.isEmpty()) { setStatus("请先填写服务器地址！", Color.RED); return; }

        setStatus("正在连接服务器...", Color.YELLOW);

        game.socketManager.connectAndCreate(url, new SocketManager.RoomListener() {
            @Override
            public void onRoomCreated(String roomCode, JSONObject data, String playerId) {
                int[][] map = parseMap(data);
                setStatus("房间创建成功！", Color.GREEN);
                game.setScreen(new LobbyScreen(game, roomCode, playerId, map, true));
            }
            @Override public void onRoomJoined(String roomCode, JSONObject data, String playerId) {}
            @Override public void onPlayerJoined(int playerCount) {}
            @Override public void onPlayerLeft(String playerId) {}
            @Override public void onGameStart(JSONObject data) {}
            @Override
            public void onError(String error) {
                setStatus("错误: " + error, Color.RED);
            }
        });
    }

    private void onJoinRoom() {
        String url  = serverUrlField.getText().trim();
        String code = roomCodeField.getText().trim().toUpperCase();

        if (url.isEmpty())       { setStatus("请先填写服务器地址！", Color.RED); return; }
        if (code.length() != 6) { setStatus("房间码必须是6位字母数字！", Color.RED); return; }

        setStatus("正在加入房间 " + code + " ...", Color.YELLOW);

        game.socketManager.connectAndJoin(url, code, new SocketManager.RoomListener() {
            @Override public void onRoomCreated(String roomCode, JSONObject data, String playerId) {}
            @Override
            public void onRoomJoined(String roomCode, JSONObject data, String playerId) {
                int[][] map = parseMap(data);
                setStatus("加入成功！", Color.GREEN);
                game.setScreen(new LobbyScreen(game, roomCode, playerId, map, false));
            }
            @Override public void onPlayerJoined(int playerCount) {}
            @Override public void onPlayerLeft(String playerId) {}
            @Override public void onGameStart(JSONObject data) {}
            @Override
            public void onError(String error) {
                setStatus("错误: " + error, Color.RED);
            }
        });
    }

    /** 将服务器返回的 map JSON数组 转成 int[][] */
    public static int[][] parseMap(JSONObject data) {
        try {
            JSONArray mapArray = data.getJSONArray("map");
            int rows = mapArray.length();
            int cols = mapArray.getJSONArray(0).length();
            int[][] map = new int[rows][cols];
            for (int y = 0; y < rows; y++) {
                JSONArray row = mapArray.getJSONArray(y);
                for (int x = 0; x < row.length(); x++) {
                    map[y][x] = row.getInt(x);
                }
            }
            return map;
        } catch (Exception e) {
            Gdx.app.error("MenuScreen", "parseMap error: " + e.getMessage());
            return new int[Config.MAP_HEIGHT][Config.MAP_WIDTH]; // 空地图兜底
        }
    }

    private void setStatus(String msg, Color color) {
        statusLabel.setText(msg);
        statusLabel.setColor(color);
    }

    // ─────────────────────────────────────────────
    //  生命周期
    // ─────────────────────────────────────────────

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.04f, 0.04f, 0.14f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        stage.dispose();
        skin.dispose();
        titleFont.dispose();
    }
}