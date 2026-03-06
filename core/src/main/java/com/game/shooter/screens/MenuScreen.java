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
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
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
    private BitmapFont uiFont;
    private BitmapFont titleFont;
    private Texture backgroundTexture;

    public MenuScreen(ShooterGame game) {
        this.game = game;
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        // 创建渐变背景（1x256 纹理，拉伸后平滑过渡）
        Pixmap bgPix = new Pixmap(1, 256, Pixmap.Format.RGBA8888);
        for (int y = 0; y < 256; y++) {
            float t = y / 255f;
            bgPix.setColor(0.1f, 0.15f, 0.3f + t * 0.2f, 1f);
            bgPix.drawPixel(0, y);
        }
        backgroundTexture = new Texture(bgPix);
        bgPix.dispose();

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

    /** 创建纯色 Drawable */
    private Drawable solidDrawable(Color color, float minWidth, float minHeight) {
        Pixmap pix = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pix.setColor(color);
        pix.fill();
        Texture tex = new Texture(pix);
        pix.dispose();
        TextureRegionDrawable drawable = new TextureRegionDrawable(new TextureRegion(tex));
        drawable.setMinWidth(minWidth);
        drawable.setMinHeight(minHeight);
        return drawable;
    }

    private Skin createSkin() {
        Skin skin = new Skin();

        // 添加字体
        skin.add("default-font", uiFont);
        skin.add("title-font", titleFont);

        // 添加各种背景 Drawable
        skin.add("input-bg", solidDrawable(new Color(0.15f, 0.2f, 0.35f, 0.9f), 200, 60));
        skin.add("input-bg-focused", solidDrawable(new Color(0.2f, 0.25f, 0.45f, 1f), 200, 60));

        // 光标
        Pixmap cursorPix = new Pixmap(3, 20, Pixmap.Format.RGBA8888);
        cursorPix.setColor(Color.WHITE);
        cursorPix.fill();
        skin.add("cursor", new TextureRegionDrawable(new Texture(cursorPix)));
        cursorPix.dispose();

        TextField.TextFieldStyle tfStyle = new TextField.TextFieldStyle();
        tfStyle.font = skin.getFont("default-font");
        tfStyle.fontColor = Color.WHITE;
        tfStyle.messageFontColor = new Color(0.7f, 0.7f, 0.7f, 0.7f);
        tfStyle.background = skin.getDrawable("input-bg");
        tfStyle.focusedBackground = skin.getDrawable("input-bg-focused");
        tfStyle.cursor = skin.getDrawable("cursor");
        skin.add("default", tfStyle);

        // 蓝色按钮
        skin.add("blue-up", solidDrawable(new Color(0.2f, 0.5f, 0.9f, 1f), 200, 70));
        skin.add("blue-down", solidDrawable(new Color(0.15f, 0.4f, 0.7f, 1f), 200, 70));

        TextButton.TextButtonStyle blueStyle = new TextButton.TextButtonStyle();
        blueStyle.font = skin.getFont("default-font");
        blueStyle.fontColor = Color.WHITE;
        blueStyle.up = skin.getDrawable("blue-up");
        blueStyle.down = skin.getDrawable("blue-down");
        skin.add("blue", blueStyle);

        // 绿色按钮
        skin.add("green-up", solidDrawable(new Color(0.15f, 0.7f, 0.2f, 1f), 200, 70));
        skin.add("green-down", solidDrawable(new Color(0.1f, 0.5f, 0.15f, 1f), 200, 70));

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
        Table root = new Table();
        root.setFillParent(true);
        root.center();
        root.pad(50);
        stage.addActor(root);

        Label title = new Label("3人射击对战", new Label.LabelStyle(skin.getFont("title-font"), new Color(0.4f, 0.9f, 1f, 1f)));
        root.add(title).colspan(2).padBottom(10).row();

        Label subtitle = new Label("非局域网实时联机 · 3人吃鸡", skin);
        subtitle.setColor(new Color(0.8f, 0.8f, 0.8f, 0.8f));
        root.add(subtitle).colspan(2).padBottom(50).row();

        root.add(new Label("服务器地址:", skin)).right().padRight(15);
        serverUrlField = new TextField(Config.DEFAULT_SERVER_URL, skin);
        serverUrlField.setMessageText("https://your-app.up.railway.app");
        root.add(serverUrlField).width(500).height(65).padBottom(25).row();

        TextButton createBtn = new TextButton("  ➕  创建房间  ", skin, "blue");
        createBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent e, float x, float y) { onCreateRoom(); }
        });
        root.add(createBtn).colspan(2).width(400).height(80).padBottom(30).row();

        Label orLabel = new Label("—— 或者加入朋友的房间 ——", skin);
        orLabel.setColor(new Color(0.7f, 0.7f, 0.7f, 0.8f));
        root.add(orLabel).colspan(2).padBottom(25).row();

        root.add(new Label("房 间 码:", skin)).right().padRight(15);
        roomCodeField = new TextField("", skin);
        roomCodeField.setMessageText("输入6位房间码 (如: AB12CD)");
        roomCodeField.setMaxLength(6);
        root.add(roomCodeField).width(500).height(65).padBottom(25).row();

        TextButton joinBtn = new TextButton("  🚪  加入房间  ", skin, "green");
        joinBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent e, float x, float y) { onJoinRoom(); }
        });
        root.add(joinBtn).colspan(2).width(400).height(80).padBottom(30).row();

        statusLabel = new Label("", skin);
        statusLabel.setColor(Color.YELLOW);
        root.add(statusLabel).colspan(2).row();
    }

    // 以下事件处理方法与之前完全相同，略...

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
        backgroundTexture.dispose();
    }
}