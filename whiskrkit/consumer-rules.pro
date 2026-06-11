# WhiskrKit consumer rules.
# kotlinx.serialization is compiler-plugin based; serializers for WhiskrKit's own
# models are looked up statically, so no broad keep rules are required.
# R8 full mode: keep the generated serializer companions of our own models.
-keepclassmembers class eu.whiskrkit.** {
    *** Companion;
}
-keepclasseswithmembers class eu.whiskrkit.** {
    kotlinx.serialization.KSerializer serializer(...);
}
