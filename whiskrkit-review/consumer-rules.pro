# WhiskrKit Play review consumer rules.
# review-ktx 2.0.2 is compiled against a newer play-services-basement than it
# declares and references this annotation, which R8 then reports as missing.
# It is only an annotation and never needed at runtime.
-dontwarn com.google.android.gms.common.annotation.NoNullnessRewrite
