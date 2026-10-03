# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
# Preserve the entry class and all its members from being obfuscated

-adaptresourcefilenames
-repackageclasses
-allowaccessmodification

# BackupCrypto: allow R8 to obfuscate the class and key fields.
# The key fragments (kf, ivF) and derived methods (keyBytes, ivBytes)
# are deliberately NOT kept - R8 renames and inlines them so the
# encryption key material is not trivially extractable from DEX.
-keep class io.github.tanakalun.llyfr.data.BackupCrypto {
    public static * encrypt(byte[]);
    public static * decrypt(byte[]);
}

# kotlinx-serialization: keep serializers for backup DTOs
-keepclassmembers class io.github.tanakalun.llyfr.data.Backup** {
    public static ** serializer(...);
    public static ** companion;
}
-keep,includedescriptorclasses class io.github.tanakalun.llyfr.data.Backup**$$serializer { *; }