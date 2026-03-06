package com.game.shooter.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
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

    public MenuScreen(ShooterGame game) {
        this.game = game;
        stage = new Stage(new ScreenViewport());
        generateFonts();      // 先生成字体
        skin = buildSkin();   // 再构建皮肤
        buildUI();
        Gdx.input.setInputProcessor(stage);
    }

    private void generateFonts() {
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/simhei.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = 48;
        parameter.characters = "你我他的一是了在人有来创建房间加入房间服务器地址0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz——➕🚪错误成功连接等待：";
        BitmapFont baseFont = generator.generateFont(parameter);
        generator.dispose();

        // 使用 baseFont 的数据和 regions 创建新的字体实例
        uiFont = new BitmapFont(baseFont.getData(), baseFont.getRegions(), baseFont.usesIntegerPositions());
        uiFont.getData().setScale(1.2f);

        titleFont = new BitmapFont(baseFont.getData(), baseFont.getRegions(), baseFont.usesIntegerPositions());
        titleFont.getData().setScale(1.8f);
    }

    private Skin buildSkin() {
        Skin skin = new Skin();

        // 白色像素纹理
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(Color.WHITE);
        pm.fill();
        skin.add("white", new Texture(pm));
        pm.dispose();

        // 添加字体到皮肤
        skin.add("default-font", uiFont);
        skin.add("title-font", titleFont);

        // Label 样式
        Label.LabelStyle labelStyle = new Label.LabelStyle();
        labelStyle.font = skin.getFont("default-font");
        labelStyle.fontColor = Color.WHITE;
        skin.add("default", labelStyle);

        // TextField 样式
        TextField.TextFieldStyle tfStyle = new TextField.TextFieldStyle();
        tfStyle.font = skin.getFont("default-font");
        tfStyle.fontColor = Color.WHITE;
        tfStyle.messageFontColor = new Color(0.6f, 0.6f, 0.6f, 1f);
        tfStyle.background = tintDrawable(skin, new Color(0.08f, 0.08f, 0.2f, 0.95f));
        tfStyle.focusedBackground = tintDrawable(skin, new Color(0.1f, 0.1f, 0.35f, 1f));
        tfStyle.cursor = tintDrawable(skin, Color.WHITE);
        skin.add("default", tfStyle);

        // 蓝色按钮样式
        TextButton.TextButtonStyle btnBlue = new TextButton.TextButtonStyle();
        btnBlue.font = skin.getFont("default-font");
        btnBlue.fontColor = Color.WHITE;
        btnBlue.up = tintDrawable(skin, new Color(0.2f, 0.5f, 0.9f, 1f));
        btnBlue.down = tintDrawable(skin, new Color(0.15f, 0.4f, 0.7f, 1f));
        btnBlue.over = tintDrawable(skin, new Color(0.25f, 0.6f, 1.0f, 1f));
        skin.add("default", btnBlue);

        // 绿色按钮样式
        TextButton.TextButtonStyle btnGreen = new TextButton.TextButtonStyle();
        btnGreen.font = skin.getFont("default-font");
        btnGreen.fontColor = Color.WHITE;
        btnGreen.up = tintDrawable(skin, new Color(0.15f, 0.6f, 0.2f, 1f));
        btnGreen.down = tintDrawable(skin, new Color(0.1f, 0.45f, 0.15f, 1f));
        btnGreen.over = tintDrawable(skin, new Color(0.2f, 0.7f, 0.3f, 1f));
        skin.add("green", btnGreen);

        return skin;
    }

    private Drawable tintDrawable(Skin skin, Color color) {
        return new TextureRegionDrawable(skin.getRegion("white")).tint(color);
    }

    private void buildUI() {
        Table root = new Table();
        root.setFillParent(true);
        root.pad(30);
        stage.addActor(root);

        Label title = new Label("  3人射击对战  ", new Label.LabelStyle(skin.getFont("title-font"), new Color(0.4f, 0.9f, 1f, 1f)));
        root.add(title).colspan(2).padBottom(10).row();

        Label subtitle = new Label("非局域网实时联机 · 3人吃鸡", skin);
        subtitle.setColor(new Color(0.7f, 0.7f, 0.7f, 1f));
        root.add(subtitle).colspan(2).padBottom(40).row();

        root.add(makeLabel("服务器地址:")).right().padRight(12);
        serverUrlField = new TextField(Config.DEFAULT_SERVER_URL, skin);
        serverUrlField.setMessageText("https://your-app.up.railway.app");
        root.add(serverUrlField).width(480).height(65).padBottom(20).row();

        TextButton createBtn = new TextButton("  ➕  创建房间  ", skin);
        createBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent e, float x, float y) {
                onCreateRoom();
            }
        });
        root.add(createBtn).colspan(2).width(450).height(80).padBottom(30).row();

        Label orLabel = makeLabel("—— 或者加入朋友的房间 ——");
        orLabel.setColor(new Color(0.6f, 0.6f, 0.6f, 1f));
        root.add(orLabel).colspan(2).padBottom(20).row();

        root.add(makeLabel("房 间 码:")).right().padRight(12);
        roomCodeField = new TextField("", skin);
        roomCodeField.setMessageText("输入6位房间码 (如: AB12CD)");
        roomCodeField.setMaxLength(6);
        root.add(roomCodeField).width(480).height(65).padBottom(20).row();

        TextButton joinBtn = new TextButton("  🚪  加入房间  ", skin, "green");
        joinBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent e, float x, float y) {
                onJoinRoom();
            }
        });
        root.add(joinBtn).colspan(2).width(450).height(80).padBottom(30).row();

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
        if (url.isEmpty()) {
            setStatus("请先填写服务器地址！", Color.RED);
            return;
        }

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
            @Override public void onError(String error) {
                setStatus("错误: " + error, Color.RED);
            }
        });
    }

    private void onJoinRoom() {
        String url = serverUrlField.getText().trim();
        String code = roomCodeField.getText().trim().toUpperCase();

        if (url.isEmpty()) {
            setStatus("请先填写服务器地址！", Color.RED);
            return;
        }
        if (code.length() != 6) {
            setStatus("房间码必须是6位字母数字！", Color.RED);
            return;
        }

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
            @Override public void onError(String error) {
                setStatus("错误: " + error, Color.RED);
            }
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
    }
}