package com.game.shooter.network;

import com.badlogic.gdx.Gdx;
import org.json.JSONObject;

import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;

/**
 * 网络管理器 —— 封装所有 Socket.IO 通信逻辑
 *
 * 使用方式：
 *   1. connectAndCreate(url, listener) —— 连接服务器并创建房间
 *   2. connectAndJoin(url, code, listener) —— 连接服务器并加入房间
 *   3. setGameStateListener(listener) —— 游戏中接收状态更新
 *   4. sendInput(dx, dy, attacking, angle) —— 每帧发送玩家操作
 *
 * 注意：Socket.IO 的事件回调运行在子线程，
 *       通过 Gdx.app.postRunnable() 把结果转回 GL线程（主线程）
 */
public class SocketManager {

    private Socket socket;

    // ===== 回调接口定义 =====

    /** 房间事件监听（大厅阶段使用） */
    public interface RoomListener {
        void onRoomCreated(String roomCode, JSONObject data, String playerId);
        void onRoomJoined(String roomCode, JSONObject data, String playerId);
        void onPlayerJoined(int playerCount);
        void onPlayerLeft(String playerId);
        void onGameStart(JSONObject data);
        void onError(String error);
    }

    /** 游戏状态监听（对战阶段使用） */
    public interface GameStateListener {
        void onGameState(JSONObject state);
        void onGameOver(String winnerId);
    }

    private RoomListener        roomListener;
    private GameStateListener   gameStateListener;
    private LobbyUpdateListener lobbyUpdateListener;

    // ===== 大厅更新监听接口（LobbyScreen专用）=====

    /** 大厅阶段：监听玩家数变化和游戏开始信号 */
    public interface LobbyUpdateListener {
        void onPlayerCountChanged(int count);
        void onGameStarting();
        void onPlayerLeft(String playerId);
    }

    // ===== 公开方法 =====

    /** 连接服务器并创建新房间 */
    public void connectAndCreate(String serverUrl, RoomListener listener) {
        this.roomListener = listener;
        doConnect(serverUrl, true, null);
    }

    /** 连接服务器并加入已有房间 */
    public void connectAndJoin(String serverUrl, String roomCode, RoomListener listener) {
        this.roomListener = listener;
        doConnect(serverUrl, false, roomCode);
    }

    /**
     * 单独更新 RoomListener，不重新连接。
     * 用于 LobbyScreen 替换掉 MenuScreen 设置的监听器
     */
    public void setRoomListener(RoomListener listener) {
        this.roomListener = listener;
    }

    /** 设置大厅更新监听（LobbyScreen 使用） */
    public void setLobbyUpdateListener(LobbyUpdateListener listener) {
        this.lobbyUpdateListener = listener;
    }

    /** 设置游戏状态监听（切换到 GameScreen 后调用） */
    public void setGameStateListener(GameStateListener listener) {
        this.gameStateListener = listener;
    }

    /** 发送玩家输入（每帧调用，20Hz节流在GameScreen中处理） */
    public void sendInput(float dx, float dy, boolean attacking, float angle) {
        if (socket == null || !socket.connected()) return;
        try {
            JSONObject inp = new JSONObject();
            inp.put("dx", dx);
            inp.put("dy", dy);
            inp.put("attacking", attacking);
            inp.put("angle", angle);
            socket.emit("player_input", inp);
        } catch (Exception e) {
            Gdx.app.error("SocketManager", "sendInput error: " + e.getMessage());
        }
    }

    /** 是否已连接 */
    public boolean isConnected() {
        return socket != null && socket.connected();
    }

    /** 断开连接并释放资源 */
    public void disconnect() {
        if (socket != null) {
            socket.off();
            socket.disconnect();
            socket.close();
            socket = null;
        }
    }

    // ===== 私有方法 =====

    private void doConnect(String serverUrl, boolean createOnConnect, String joinCode) {
        // 如果已有连接先断开
        if (socket != null) {
            socket.off();
            socket.disconnect();
            socket.close();
        }

        try {
            IO.Options opts = IO.Options.builder()
                    .setTransports(new String[]{"websocket"})
                    .setReconnection(true)
                    .setReconnectionAttempts(5)
                    .build();

            socket = IO.socket(serverUrl, opts);
            setupListeners(createOnConnect, joinCode);
            socket.connect();

        } catch (URISyntaxException e) {
            Gdx.app.error("SocketManager", "Invalid URL: " + serverUrl);
            if (roomListener != null) {
                final String msg = "服务器地址格式错误: " + e.getMessage();
                Gdx.app.postRunnable(() -> roomListener.onError(msg));
            }
        }
    }

    private void setupListeners(boolean createOnConnect, String joinCode) {

        // ---- 连接成功 ----
        socket.on(Socket.EVENT_CONNECT, args -> {
            Gdx.app.log("SocketManager", "Connected!");
            if (createOnConnect) {
                socket.emit("create_room");
            } else if (joinCode != null) {
                try {
                    JSONObject data = new JSONObject();
                    data.put("roomCode", joinCode);
                    socket.emit("join_room", data);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        // ---- 连接断开 ----
        socket.on(Socket.EVENT_DISCONNECT, args ->
                Gdx.app.log("SocketManager", "Disconnected"));

        // ---- 连接错误 ----
        socket.on(Socket.EVENT_CONNECT_ERROR, args -> {
            Gdx.app.error("SocketManager", "Connect error");
            if (roomListener != null) {
                Gdx.app.postRunnable(() -> roomListener.onError("连接失败，请检查服务器地址"));
            }
        });

        // ---- 房间创建成功 ----
        socket.on("room_created", args -> {
            if (roomListener == null || args.length == 0) return;
            try {
                JSONObject data = (JSONObject) args[0];
                String roomCode = data.getString("roomCode");
                String playerId = data.getString("playerId");
                Gdx.app.postRunnable(() -> roomListener.onRoomCreated(roomCode, data, playerId));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // ---- 加入房间成功 ----
        socket.on("room_joined", args -> {
            if (roomListener == null || args.length == 0) return;
            try {
                JSONObject data = (JSONObject) args[0];
                String roomCode = data.getString("roomCode");
                String playerId = data.getString("playerId");
                Gdx.app.postRunnable(() -> roomListener.onRoomJoined(roomCode, data, playerId));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // ---- 加入失败 ----
        socket.on("join_error", args -> {
            if (roomListener == null || args.length == 0) return;
            try {
                JSONObject data = (JSONObject) args[0];
                String error = data.getString("error");
                Gdx.app.postRunnable(() -> roomListener.onError(error));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // ---- 有新玩家加入 ----
        socket.on("player_joined", args -> {
            if (args.length == 0) return;
            try {
                JSONObject data = (JSONObject) args[0];
                int count = data.getInt("playerCount");
                Gdx.app.postRunnable(() -> {
                    if (roomListener != null)        roomListener.onPlayerJoined(count);
                    if (lobbyUpdateListener != null) lobbyUpdateListener.onPlayerCountChanged(count);
                });
            } catch (Exception e) { e.printStackTrace(); }
        });

        // ---- 玩家离开 ----
        socket.on("player_left", args -> {
            if (args.length == 0) return;
            try {
                JSONObject data = (JSONObject) args[0];
                String pid = data.getString("playerId");
                Gdx.app.postRunnable(() -> {
                    if (roomListener != null)        roomListener.onPlayerLeft(pid);
                    if (lobbyUpdateListener != null) lobbyUpdateListener.onPlayerLeft(pid);
                });
            } catch (Exception e) { e.printStackTrace(); }
        });

        // ---- 游戏开始 ----
        socket.on("game_start", args -> {
            if (args.length == 0) return;
            JSONObject data = (JSONObject) args[0];
            Gdx.app.postRunnable(() -> {
                if (roomListener != null)        roomListener.onGameStart(data);
                if (lobbyUpdateListener != null) lobbyUpdateListener.onGameStarting();
            });
        });

        // ---- 游戏状态更新（高频，20Hz）----
        socket.on("game_state", args -> {
            if (gameStateListener == null || args.length == 0) return;
            JSONObject state = (JSONObject) args[0];
            // 直接在GL线程中处理，不需要 postRunnable（已经从socket线程发来）
            Gdx.app.postRunnable(() -> gameStateListener.onGameState(state));
        });

        // ---- 游戏结束 ----
        socket.on("game_over", args -> {
            if (gameStateListener == null || args.length == 0) return;
            try {
                JSONObject data = (JSONObject) args[0];
                String winnerId = data.optString("winnerId", "");
                Gdx.app.postRunnable(() -> gameStateListener.onGameOver(winnerId));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
