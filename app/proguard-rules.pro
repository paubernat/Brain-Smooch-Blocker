# BrainSmooch ProGuard Rules
-keepclassmembers class * extends android.app.admin.DeviceAdminReceiver {
    public *;
}
-keep class com.brainsmooch.receiver.** { *; }
-keep class com.brainsmooch.service.** { *; }
