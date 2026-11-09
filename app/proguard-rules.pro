# Keep kotlinx.serialization generated serializers.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.slicehouse.pos.** {
    *** Companion;
}
-keepclasseswithmembers class com.slicehouse.pos.** {
    kotlinx.serialization.KSerializer serializer(...);
}
