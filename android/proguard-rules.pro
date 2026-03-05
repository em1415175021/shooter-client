# Socket.IO 不能被混淆
-keep class io.socket.** { *; }
-keep class okhttp3.** { *; }
-keep class okio.** { *; }

# LibGDX 不能被混淆
-keep class com.badlogic.gdx.** { *; }
-keep class com.game.shooter.** { *; }

-dontwarn io.socket.**
-dontwarn okhttp3.**
-dontwarn okio.**
