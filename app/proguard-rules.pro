# Zcut release rules.
# Keep serialized models readable across app versions.
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.zaaam.zcut.**$$serializer { *; }
-keepclassmembers class com.zaaam.zcut.** {
    *** Companion;
}
-keepclasseswithmembers class com.zaaam.zcut.** {
    kotlinx.serialization.KSerializer serializer(...);
}
