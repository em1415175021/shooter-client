package com.game.shooter.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.game.shooter.Config;
import com.game.shooter.ShooterGame;
import com.game.shooter.network.SocketManager;
import org.json.JSONArray;
import org.json.JSONObject;

public class MenuScreen extends ScreenAdapter {

    private final ShooterGame game;
    private Stage stage;
    private Skin skin;
    private TextField serverUrlField;
    private TextField roomCodeField;
    private Label statusLabel;

    // 字体
    private BitmapFont uiFont;
    private BitmapFont titleFont;

    // 背景纹理
    private Texture backgroundTexture;

    public MenuScreen(ShooterGame game) {
        this.game = game;
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        // 加载背景图（如果存在）
        if (Gdx.files.internal("bg.jpg").exists()) {
            backgroundTexture = new Texture("bg.jpg");
        } else {
            // 如果没有背景图，生成渐变纹理（从深蓝到黑色）
            Pixmap bgPixmap = new Pixmap(1, 256, Pixmap.Format.RGBA8888);
            for (int y = 0; y < 256; y++) {
                float t = y / 255f;
                bgPixmap.setColor(0.1f, 0.15f, 0.3f + t * 0.2f, 1f);
                bgPixmap.drawPixel(0, y);
            }
            backgroundTexture = new Texture(bgPixmap);
            bgPixmap.dispose();
        }

        generateFonts();
        skin = createSkin();
        buildUI();
    }

    private void generateFonts() {
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/simhei.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter param = new FreeTypeFontGenerator.FreeTypeFontParameter();
        param.size = 48;
        param.characters = "你我他的一是了在人有来创建房间加入房间服务器地址0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz——➕🚪错误成功连接等待：";
        BitmapFont baseFont = generator.generateFont(param);
        generator.dispose();

        uiFont = new BitmapFont(baseFont.getData(), baseFont.getRegions(), baseFont.usesIntegerPositions());
        uiFont.getData().setScale(1.2f);

        titleFont = new BitmapFont(baseFont.getData(), baseFont.getRegions(), baseFont.usesIntegerPositions());
        titleFont.getData().setScale(2.2f);
    }

    private Skin createSkin() {
        Skin skin = new Skin();

        // 添加字体
        skin.add("default-font", uiFont);
        skin.add("title-font", titleFont);

        // 基础白色纹理（用于生成九宫格或纯色）
        Pixmap whitePix = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        whitePix.setColor(Color.WHITE);
        whitePix.fill();
        skin.add("white", new Texture(whitePix));
        whitePix.dispose();

        // ========== 输入框样式 ==========
        // 创建圆角矩形九宫格（4个角是圆角）
        Pixmap inputBgPix = new Pixmap(20, 20, Pixmap.Format.RGBA8888);
        inputBgPix.setColor(0.15f, 0.2f, 0.35f, 0.9f); // 半透明深蓝
        inputBgPix.fillRectangle(2, 2, 16, 16);
        // 描边
        inputBgPix.setColor(0.4f, 0.7f, 1f, 0.8f);
        for (int i = 0; i < 2; i++) {
            inputBgPix.drawRectangle(i, i, 20 - i * 2, 20 - i * 2);
        }
        NinePatchDrawable inputBg = new NinePatchDrawable(new com.badlogic.gdx.graphics.g2d.NinePatch(new Texture(inputBgPix), 5, 5, 5, 5));
        inputBg.setMinWidth(200);
        inputBg.setMinHeight(60);
        skin.add("input-bg", inputBg);
        inputBgPix.dispose();

        TextField.TextFieldStyle tfStyle = new TextField.TextFieldStyle();
        tfStyle.font = skin.getFont("default-font");
        tfStyle.fontColor = Color.WHITE;
        tfStyle.messageFontColor = new Color(0.7f, 0.7f, 0.7f, 0.7f);
        tfStyle.background = skin.getDrawable("input-bg");
        tfStyle.focusedBackground = skin.getDrawable("input-bg"); // 可以用同一个，或不同颜色
        // 游标（白色竖线）
        Pixmap cursorPix = new Pixmap(3, 20, Pixmap.Format.RGBA8888);
        cursorPix.setColor(Color.WHITE);
        cursorPix.fill();
        skin.add("cursor", new TextureRegionDrawable(new Texture(cursorPix)));
        cursorPix.dispose();
        tfStyle.cursor = skin.getDrawable("cursor");
        skin.add("default", tfStyle);

        // ========== 按钮样式 ==========
        // 蓝色按钮（创建房间）
        Pixmap blueBtnUp = new Pixmap(30, 30, Pixmap.Format.RGBA8888);
        blueBtnUp.setColor(0.2f, 0.5f, 0.9f, 1f);
        blueBtnUp.fillCircle(15, 15, 15);
        blueBtnUp.setColor(1f, 1f, 1f, 0.4f);
        blueBtnUp.fillCircle(12, 18, 4); // 高光
        NinePatchDrawable blueUp = new NinePatchDrawable(new com.badlogic.gdx.graphics.g2d.NinePatch(new Texture(blueBtnUp), 10, 10, 10, 10));
        blueUp.setMinWidth(200);
        blueUp.setMinHeight(70);
        skin.add("blue-up", blueUp);
        blueBtnUp.dispose();

        Pixmap blueBtnDown = new Pixmap(30, 30, Pixmap.Format.RGBA8888);
        blueBtnDown.setColor(0.15f, 0.4f, 0.7f, 1f);
        blueBtnDown.fillCircle(15, 15, 15);
        NinePatchDrawable blueDown = new NinePatchDrawable(new com.badlogic.gdx.graphics.g2d.NinePatch(new Texture(blueBtnDown), 10, 10, 10, 10));
        blueDown.setMinWidth(200);
        blueDown.setMinHeight(70);
        skin.add("blue-down", blueDown);
        blueBtnDown.dispose();

        TextButton.TextButtonStyle blueStyle = new TextButton.TextButtonStyle();
        blueStyle.font = skin.getFont("default-font");
        blueStyle.fontColor = Color.WHITE;
        blueStyle.up = skin.getDrawable("blue-up");
        blueStyle.down = skin.getDrawable("blue-down");
        skin.add("blue", blueStyle);

        // 绿色按钮（加入房间）
        Pixmap greenBtnUp = new Pixmap(30, 30, Pixmap.Format.RGBA8888);
        greenBtnUp.setColor(0.15f, 0.7f, 0.2f, 1f);
        greenBtnUp.fillCircle(15, 15, 15);
        greenBtnUp.setColor(1f, 1f, 1f, 0.4f);
        greenBtnUp.fillCircle(12, 18, 4);
        NinePatchDrawable greenUp = new NinePatchDrawable(new com.badlogic.gdx.graphics.g2d.NinePatch(new Texture(greenBtnUp), 10, 10, 10, 10));
        greenUp.setMinWidth(200);
        greenUp.setMinHeight(70);
        skin.add("green-up", greenUp);
        greenBtnUp.dispose();

        Pixmap greenBtnDown = new Pixmap(30, 30, Pixmap.Format.RGBA8888);
        greenBtnDown.setColor(0.1f, 0.5f, 0.15f, 1f);
        greenBtnDown.fillCircle(15, 15, 15);
        NinePatchDrawable greenDown = new NinePatchDrawable(new com.badlogic.gdx.graphics.g2d.NinePatch(new Texture(greenBtnDown), 10, 10, 10, 10));
        greenDown.setMinWidth(200);
        greenDown.setMinHeight(70);
        skin.add("green-down", greenDown);
        greenBtnDown.dispose();

        TextButton.TextButtonStyle greenStyle = new TextButton.TextButtonStyle();
        greenStyle.font = skin.getFont("default-font");
        greenStyle.fontColor = Color.WHITE;
        greenStyle.up = skin.getDrawable("green-up");
        greenStyle.down = skin.getDrawable("green-down");
        skin.add("green", greenStyle);

        // Label样式
        Label.LabelStyle labelStyle = new Label.LabelStyle();
        labelStyle.font = skin.getFont("default-font");
        labelStyle.fontColor = Color.WHITE;
        skin.add("default", labelStyle);

        return skin;
    }

    private void buildUI() {
        // 主布局表格
        Table root = new Table();
        root.setFillParent(true);
        root.center();
        root.pad(50);
        stage.addActor(root);

        // 标题（带发光效果：加一层半透明副本）
        Label titleShadow = new Label("3人射击对战", new Label.LabelStyle(skin.getFont("title-font"), new Color(0.2f, 0.2f, 0.3f, 0.5f)));
        root.add(titleShadow).colspan(2).padBottom(5).row();
        Label title = new Label("3人射击对战", new Label.LabelStyle(skin.getFont("title-font"), new Color(0.4f, 0.9f, 1f, 1f)));
        root.add(title).colspan(2).padBottom(10).row();

        // 副标题
        Label subtitle = new Label("非局域网实时联机 · 3人吃鸡", skin);
        subtitle.setColor(new Color(0.8f, 0.8f, 0.8f, 0.8f));
        root.add(subtitle).colspan(2).padBottom(50).row();

        // 服务器地址
        root.add(makeLabel("服务器地址:")).right().padRight(15);
        serverUrlField = new TextField(Config.DEFAULT_SERVER_URL, skin);
        serverUrlField.setMessageText("https://your-app.up.railway.app");
        root.add(serverUrlField).width(500).height(65).padBottom(25).row();

        // 创建房间按钮
        TextButton createBtn = new TextButton("  ➕  创建房间  ", skin, "blue");
        createBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent e, float x, float y) { onCreateRoom(); }
        });
        root.add(createBtn).colspan(2).width(400).height(80).padBottom(30).row();

        // 分隔线
        Label orLabel = makeLabel("—— 或者加入朋友的房间 ——");
        orLabel.setColor(new Color(0.7f, 0.7f, 0.7f, 0.8f));
        root.add(orLabel).colspan(2).padBottom(25).row();

        // 房间码输入
        root.add(makeLabel("房 间 码:")).right().padRight(15);
        roomCodeField = new TextField("", skin);
        roomCodeField.setMessageText("输入6位房间码 (如: AB12CD)");
        roomCodeField.setMaxLength(6);
        root.add(roomCodeField).width(500).height(65).padBottom(25).row();

        // 加入房间按钮
        TextButton joinBtn = new TextButton("  🚪  加入房间  ", skin, "green");
        joinBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent e, float x, float y) { onJoinRoom(); }
        });
        root.add(joinBtn).colspan(2).width(400).height(80).padBottom(30).row();

        // 状态标签
        statusLabel = new Label("", skin);
        statusLabel.setColor(Color.YELLOW);
        root.add(statusLabel).colspan(2).row();
    }

    private Label makeLabel(String text) {
        return new Label(text, skin);
    }

    // 事件处理方法（与之前相同）
    private void onCreateRoom() {
        String url = serverUrlField.getText().trim();
        if (url.isEmpty()) { setStatus("请先填写服务器地址！", Color.RED); return; }
        setStatus("正在连接服务器...", Color.YELLOW);
        game.socketManager.connectAndCreate(url, new SocketManager.RoomListener() {
            @Override public void onRoomCreated(String roomCode, JSONObject data, String playerId) {
                int[][] map = parseMap(data);
                setStatus("房间创建成功！", Color.GREEN);
                game.setScreen(new LobbyScreen(game, roomCode, playerId, map, true));
            }
            @Override public void onRoomJoined(String roomCode, JSONObject data, String playerId) {}
            @Override public void onPlayerJoined(int playerCount) {}
            @Override public void onPlayerLeft(String playerId) {}
            @Override public void onGameStart(JSONObject data) {}
            @Override public void onError(String error) { setStatus("错误: " + error, Color.RED); }
        });
    }

    private void onJoinRoom() {
        String url = serverUrlField.getText().trim();
        String code = roomCodeField.getText().trim().toUpperCase();
        if (url.isEmpty()) { setStatus("请先填写服务器地址！", Color.RED); return; }
        if (code.length() != 6) { setStatus("房间码必须是6位字母数字！", Color.RED); return; }
        setStatus("正在加入房间 " + code + " ...", Color.YELLOW);
        game.socketManager.connectAndJoin(url, code, new SocketManager.RoomListener() {
            @Override public void onRoomCreated(String roomCode, JSONObject data, String playerId) {}
            @Override public void onRoomJoined(String roomCode, JSONObject data, String playerId) {
                int[][] map = parseMap(data);
                setStatus("加入成功！", Color.GREEN);
                game.setScreen(new LobbyScreen(game, roomCode, playerId, map, false));
            }
            @Override public void onPlayerJoined(int playerCount) {}
            @Override public void onPlayerLeft(String playerId) {}
            @Override public void onGameStart(JSONObject data) {}
            @Override public void onError(String error) { setStatus("错误: " + error, Color.RED); }
        });
    }

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
            return new int[Config.MAP_HEIGHT][Config.MAP_WIDTH];
        }
    }

    private void setStatus(String msg, Color color) {
        statusLabel.setText(msg);
        statusLabel.setColor(color);
    }

    @Override
    public void render(float delta) {
        // 绘制背景
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.getBatch().begin();
        stage.getBatch().draw(backgroundTexture, 0, 0, stage.getViewport().getScreenWidth(), stage.getViewport().getScreenHeight());
        stage.getBatch().end();

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
        if (backgroundTexture != null) backgroundTexture.dispose();
    }
}