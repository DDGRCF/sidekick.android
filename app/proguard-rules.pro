# Keep Kotlin serialization metadata & generated serializers
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault,Signature,InnerClasses,EnclosingMethod

-keep @kotlinx.serialization.Serializable class dev.ddgrcf.sidekick.**
-keepclassmembers @kotlinx.serialization.Serializable class dev.ddgrcf.sidekick.** {
    static final *;
}
-keepclassmembers class dev.ddgrcf.sidekick.** {
    @kotlinx.serialization.SerialName *;
}
-keepclassmembers class dev.ddgrcf.sidekick.**$Companion {
    public kotlinx.serialization.KSerializer serializer(...);
}
-keep class dev.ddgrcf.sidekick.**$*$serializer { *; }

# Keep Wire generated Protobuf messages & adapters
-keep class * extends com.squareup.wire.Message {
    public static final com.squareup.wire.ProtoAdapter ADAPTER;
}
-keep class dev.ddgrcf.sidekick.core.protocol.** { *; }

# Keep Room 3 Database & generated _Impl classes
-keep class * extends androidx.room3.RoomDatabase {
    <init>();
}
-keep class dev.ddgrcf.sidekick.core.store.**_Impl {
    <init>(...);
}

# Keep WorkManager workers instantiated via reflection
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# Harmless optional JVM / third-party class references pulled by Tink, BouncyCastle, and OkHttp
-dontwarn org.bouncycastle.jsse.**
-dontwarn org.bouncycastle.pqc.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
-dontwarn javax.naming.**
-dontwarn java.lang.instrument.**
-dontwarn sun.misc.Unsafe
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
