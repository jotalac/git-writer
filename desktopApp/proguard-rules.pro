# Desktop-only additions to proguard/shared-rules.pro.

# JNA resolves the interface method names of its callback structs by reflection.
-keep class com.sun.jna.** { *; }
-keepclassmembers class * extends com.sun.jna.** { public *; }
-dontwarn com.sun.jna.**

# Skiko is loaded as a native library and reached through JNI.
-keep class org.jetbrains.skiko.** { *; }
-dontwarn org.jetbrains.skiko.**

# Compose desktop reads its resources from the classpath by name.
-keep class git_writer.shared.generated.resources.** { *; }

# --- optional runtime integrations that are not on the desktop classpath ------
# Ktor references these for TLS providers and GraalVM native-image support; their absence is
# expected and handled at runtime, so the unresolved references are not errors.
-dontwarn com.oracle.svm.**
-dontwarn org.graalvm.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# --- inconsistent third-party input -------------------------------------------
# The latex renderer and filekit were compiled against a different Skiko build and reference members
# that do not exist in the Skiko this project resolves, which makes ProGuard refuse to run:
#   LatexExporter_jvmKt -> org.jetbrains.skia.Data encodeToData(EncodedImageFormat, int)
#   LatexTokenizer$Companion -> boolean[] TEXT_STOP_CHARS
# These paths are never executed, and the same mismatch would throw NoSuchMethodError without
# ProGuard too, so it is a dependency-version issue rather than something shrinking introduced.
-ignorewarnings
