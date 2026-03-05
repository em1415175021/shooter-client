# 🔫 3人实时射击对战游戏

一个基于 **LibGDX + Socket.IO** 的 Android 实时联机2D射击游戏，支持非局域网（互联网）3人同时对战。

---

## 🎮 游戏玩法

| 操作 | 说明 |
|------|------|
| 左侧滑动 | 虚拟摇杆控制移动方向 |
| 右下红圈 | 攻击键（持续按住=连续攻击） |
| 走过金箱子 | 自动拾取，随机获得枪/刀/盾 |

### 武器系统
| 武器 | 伤害 | 射程 | 冷却 | 特性 |
|------|------|------|------|------|
| 🔫 枪 | 20 | 450px | 0.4s | 远程子弹，穿越障碍 |
| 🔪 刀 | 50 | 70px | 0.6s | 近战高伤害 |
| 🛡 盾 | 15 | 65px | 0.7s | 近战低伤+减少35%受到的伤害 |
| 👊 徒手 | 10 | 55px | 0.8s | 初始状态 |

### 游戏规则
- **最后存活者获胜**
- 3人满员自动开始（3秒倒计时）
- 服务器权威验证（防作弊）

---

## 📁 项目结构

```
桌面/
├── shooter-server/          ← Node.js 游戏服务器
│   ├── server.js            ← 核心服务器逻辑（地图生成、物理、广播）
│   └── package.json
│
├── shooter-client/          ← Android LibGDX 客户端
│   ├── core/src/main/java/com/game/shooter/
│   │   ├── Config.java              ← 全局配置（服务器URL等）
│   │   ├── ShooterGame.java         ← LibGDX入口
│   │   ├── network/SocketManager.java ← Socket.IO网络管理
│   │   └── screens/
│   │       ├── MenuScreen.java      ← 主菜单（创建/加入房间）
│   │       ├── LobbyScreen.java     ← 等待大厅
│   │       └── GameScreen.java      ← 游戏主界面（渲染+输入）
│   └── android/                     ← Android模块
│
└── README.md
```

---

## 🚀 第一步：部署服务器到 Railway（免费）

Railway 是一个免费的云平台，用来运行你的游戏服务器。

### 1. 注册 Railway
1. 打开 [railway.app](https://railway.app)
2. 点击 **「Start a New Project」**，用 GitHub 账号登录

### 2. 创建 GitHub 仓库并上传服务器代码
```bash
# 在 shooter-server 文件夹中打开命令行
cd C:\Users\sufen\Desktop\shooter-server

# 初始化 git
git init
git add .
git commit -m "初始化游戏服务器"

# 在 GitHub 创建新仓库后推送（替换 YOUR_USERNAME）
git remote add origin https://github.com/YOUR_USERNAME/shooter-server.git
git push -u origin main
```

### 3. 部署到 Railway
1. 在 Railway 控制台点击 **「New Project」→「Deploy from GitHub Repo」**
2. 选择刚才创建的 `shooter-server` 仓库
3. Railway 会自动检测 Node.js 并执行 `npm start`
4. 部署完成后，点击 **「Settings」→「Domains」→「Generate Domain」**
5. 复制你的服务器地址（格式：`https://xxxxxx.up.railway.app`）

### 4. 记录服务器地址
```
你的服务器地址：https://________.up.railway.app
```

---

## 📱 第二步：构建 Android 客户端

### 环境准备
- 安装 [Android Studio](https://developer.android.com/studio)（含 Android SDK）
- JDK 11 或更高版本

### 1. 修改服务器地址
打开文件：`shooter-client/core/src/main/java/com/game/shooter/Config.java`

```java
// 修改这一行，填入你的 Railway 地址
public static final String DEFAULT_SERVER_URL = "https://你的应用.up.railway.app";
```

### 2. 用 Android Studio 打开项目
1. 启动 Android Studio
2. 点击 **「Open」**
3. 选择 `C:\Users\sufen\Desktop\shooter-client` 文件夹
4. 等待 Gradle 同步（首次可能需要几分钟，会自动下载依赖）

### 3. 构建 APK
1. 菜单栏点击 **「Build」→「Build Bundle(s)/APK(s)」→「Build APK(s)」**
2. APK 生成在：`shooter-client/android/build/outputs/apk/debug/android-debug.apk`

### 4. 安装到手机
```bash
# 方法1：直接拷贝 APK 到手机安装
# 方法2：用 ADB
adb install shooter-client/android/build/outputs/apk/debug/android-debug.apk
```

---

## 👥 第三步：开始游戏

1. **3个玩家** 各自在手机上安装并打开 APK
2. **玩家A（房主）**：
   - 输入服务器地址
   - 点击「创建房间」
   - 把显示的 **6位房间码** 发给其他两位玩家（通过微信等）
3. **玩家B、C**：
   - 输入相同的服务器地址和房间码
   - 点击「加入房间」
4. 3人全部进入大厅后，**自动倒计时3秒开始游戏！**

---

## 🛠️ 常见问题

### Q: 构建时出现 Gradle 错误
A: 在 Android Studio 中点击 **「File」→「Invalidate Caches / Restart」**，然后重新同步

### Q: 连接服务器失败
A: 
- 确认 Railway 服务器已成功部署（访问 URL 应看到 "Shooter Game Server Running!"）
- 确认 `Config.java` 中的 URL 填写正确，末尾不要有斜杠 `/`
- 确认手机网络可以访问外网

### Q: 大厅等待但人数不更新
A: 检查所有玩家是否填写了相同的服务器地址和房间码

### Q: Railway 免费额度说明
A: Railway 免费版每月有 $5 免费额度，对于小游戏测试完全够用

---

## 🔧 技术栈

| 层 | 技术 |
|----|------|
| 游戏客户端 | LibGDX 1.12.0 (Java) |
| 游戏服务器 | Node.js + Socket.IO 4.x |
| 网络协议 | WebSocket（实时双向通信）|
| 服务器托管 | Railway（免费云平台）|
| 目标平台 | Android 5.0+ (minSdk 21) |
| 服务器刷新率 | 20 TPS（每秒20次状态广播）|
| 客户端输入率 | 20 Hz |

---

## 📐 地图说明

- 地图大小：50×30 格，每格32像素（即 1600×960 世界坐标）
- **绿色**：可通行草地
- **灰色**：不可穿越墙壁
- **棕色**：不可穿越岩石
- **金色箱子**：走过自动拾取武器（随机枪/刀/盾）
- 3个固定出生点分布在地图三个角落

---

*由 AI 辅助生成 · LibGDX + Socket.IO · 2026*
