# Retrofit / OkHttp keep the service interfaces and their kotlinx.serialization models.
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class pl.foodhub.pos.**$$serializer { *; }
-keepclassmembers class pl.foodhub.pos.** {
    *** Companion;
}
-keepclasseswithmembers class pl.foodhub.pos.** {
    kotlinx.serialization.KSerializer serializer(...);
}
