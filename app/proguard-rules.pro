# Think Launcher - ProGuard/R8 rules
# Keep SettingsActivity reachable via reflection (Class.forName in
# MainActivity, GestureHandler and the app launcher screens).
-keep class org.matiasdesu.thinklauncherv2.settings.SettingsActivity { *; }

# Keep class names for anything referenced by name through reflection.
-keepnames class org.matiasdesu.thinklauncherv2.** { *; }

# Keep entry points referenced from the manifest (activities, services,
# receivers) even when not referenced from code.
-keep public class * extends android.app.Activity { *; }
-keep public class * extends android.app.Service { *; }
-keep public class * extends android.content.BroadcastReceiver { *; }
-keep public class * extends android.accessibilityservice.AccessibilityService { *; }
