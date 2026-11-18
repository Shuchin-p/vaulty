# Vaultaire ProGuard / R8 rules.
# The app is security-sensitive: keep rules minimal and explicit.

# Kotlin metadata (reflection-safe defaults)
-keepattributes *Annotation*, InnerClasses, Signature, Exceptions

# Keep the application and activity entry points referenced from the manifest.
-keep class com.vaultaire.android.VaultaireApplication { *; }
-keep class com.vaultaire.android.MainActivity { *; }

# Crypto engines rely on JCA provider lookups by algorithm name.
-keep class javax.crypto.** { *; }
-keep class java.security.** { *; }