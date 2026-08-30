-keepattributes *Annotation*, InnerClasses
-keepclassmembers class kotlinx.serialization.json.** { ***; }
-keep,includedescriptorclasses class com.example.minimaxcreator.**$$serializer { *; }
-keepclassmembers class com.example.minimaxcreator.** {
    *** Companion;
}
-keep class com.example.minimaxcreator.data.local.db.** { *; }
-keep class com.example.minimaxcreator.data.remote.dto.** { *; }
-dontwarn kotlinx.serialization.**
-dontwarn javax.annotation.**
