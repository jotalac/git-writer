# Rules shared by the Android and desktop release builds.
#
# Deliberately broader than strictly necessary in places. A library that resolves classes or members
# reflectively does not fail while shrinking - it fails at runtime, on a user's machine, usually on
# startup or the first network call. For those libraries correctness beats a smaller binary.

# Keep the metadata annotations and generic signatures that reflection and serialization read.
-keepattributes *Annotation*,InnerClasses,Signature,EnclosingMethod,RuntimeVisibleAnnotations,AnnotationDefault
-keep class kotlin.Metadata { *; }

-dontwarn kotlin.**
-dontwarn org.jetbrains.annotations.**
-dontwarn org.slf4j.**
-dontwarn java.lang.management.**
-dontwarn javax.management.**
-dontwarn org.ietf.jgss.**

# JNI: Skiko, JNA and the spell checker bridges resolve native methods by name.
-keepclasseswithmembernames class * {
    native <methods>;
}

# --- kotlinx.serialization ---------------------------------------------------
# The navigation routes are @Serializable, and their serializers are looked up by name at runtime.
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# --- Koin --------------------------------------------------------------------
# Definitions are indexed by class, and some lookups go through reflection.
-keep class org.koin.** { *; }
-dontwarn org.koin.**

# --- Ktor --------------------------------------------------------------------
-dontwarn io.ktor.**
-dontwarn org.eclipse.jetty.**
# engines and content converters are discovered by name
-keep class io.ktor.client.engine.** { *; }
-keep class io.ktor.serialization.** { *; }

# --- JGit --------------------------------------------------------------------
# Transports, protocols and file systems are loaded through ServiceLoader, and several are named in
# strings. This is the highest risk area of the whole build: exercise clone, pull and push on a
# minified build before shipping.
-keep class org.eclipse.jgit.transport.** { *; }
-keep class org.eclipse.jgit.internal.storage.** { *; }
-keep class org.eclipse.jgit.util.FS { *; }
-keep class org.eclipse.jgit.util.FS$* { *; }
-keep class * implements org.eclipse.jgit.transport.Transport { *; }
-keep class * implements org.eclipse.jgit.transport.TransportProtocol { *; }
-dontwarn org.eclipse.jgit.**

# JGit's message translation (NLS) reflects over its own bundle classes three ways, and all three have
# to survive or the first pull/push fails:
#   ResourceBundle.getBundle(bundleClass.getName())    -> renaming the class loses <Name>.properties
#   getString(field.getName()) for every public field  -> the field names ARE the resource keys
#   type.getDeclaredConstructor().newInstance()        -> the no-arg constructor must exist
# Without this, PullCommand.call() dies with
#   java.lang.NoSuchMethodException: <renamed>.<init> []
# from org.eclipse.jgit.nls.GlobalBundleCache.lookupBundle.
-keep class * extends org.eclipse.jgit.nls.TranslationBundle {
    <init>();
    public java.lang.String *;
}

# --- Room --------------------------------------------------------------------
# Room generates an implementation per database and looks it up by name.
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep @androidx.room.Entity class * { *; }
-dontwarn androidx.room.**

# --- DataStore ---------------------------------------------------------------
-dontwarn androidx.datastore.**

# --- Compose -----------------------------------------------------------------
-dontwarn androidx.compose.**

# --- spell checker -----------------------------------------------------------
# Its bridges are JNI plus reflection over the loaded session.
-keep class dev.nucleusframework.spellcheck.** { *; }
