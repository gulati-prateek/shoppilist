# Gson uses generic type information stored in a class file when working with
# fields. Proguard removes such information by default, so configure it to keep all of it
# (as recommended by https://github.com/google/gson#proguard).
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses
-dontwarn sun.misc.Unsafe

# Gson specific classes
-keep class com.google.gson.stream.** { *; }
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# Room, Hilt, and Firebase already ship their own consumer proguard rules via their AARs,
# so no extra keep rules are needed for them here.
