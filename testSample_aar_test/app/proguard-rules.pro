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

# 라이브러리에서 사용하는 클래스들 보존
-keep class com.example.testsample.** { *; }
-keepclassmembers class com.example.testsample.** { *; }

# 사용하지 않는 클래스/메서드 제거
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*

# 외부 노출 인터페이스 보호
-keep public class com.example.testsample.MainActivity { *; }

# Native 메소드 보호 (필요한 경우)
-keepclasseswithmembernames class * {
    native <methods>;
}