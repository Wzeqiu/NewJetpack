# #####################################################################################
# # 通用Android Proguard配置
# #####################################################################################

# ######################################
# # 基本优化选项
# ######################################

# 混淆时所采用的算法
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*

# 指定代码的压缩级别，0~7，默认为5
-optimizationpasses 5

# 混淆时不使用大小写混合，混淆后的类名为全小写
-dontusemixedcaseclassnames

# 指定不去忽略非公共的库类。
-dontskipnonpubliclibraryclasses

# 指定不去忽略非公共的库类的成员。
-dontskipnonpubliclibraryclassmembers

# 不做预校验，preverify是proguard的四个步骤之一，Android不需要preverify，去掉这一步能够加快混淆速度。
-dontpreverify

# 指定混淆是采用的算法，后面的参数是一个过滤器，在这里我们指定混淆的算法，该算法中保留了所有异常、注解、签名等。
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# 混淆后保持源文件名和行号不变，方便调试
-keepattributes SourceFile,LineNumberTable

# 移除Android日志代码
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}

# ######################################
# # 保留Android核心组件
# ######################################

# 保留四大组件，自定义的Application
# 任何Activity的子类不被混淆
-keep public class * extends android.app.Activity
# 任何Application的子类不被混淆
-keep public class * extends android.app.Application
# 任何Service的子类不被混淆
-keep public class * extends android.app.Service
# 任何ContentProvider的子类不被混淆
-keep public class * extends android.content.ContentProvider
# 任何BroadcastReceiver的子类不被混淆
-keep public class * extends android.content.BroadcastReceiver

# 保留support下的所有类及其成员
-keep class android.support.** { *; }
-keep class androidx.** { *; }
-keep interface androidx.** { *; }

# 保留我们自定义的 View
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public void set*(...);
}

# 保留 R$* 类中的所有静态字段
-keepclassmembers class **.R$* {
    public static <fields>;
}

# 保留所有native方法
-keepclasseswithmembernames class * {
    native <methods>;
}

# 保留Activity中的方法参数是View的方法，例如onClick(View v)
-keepclassmembers class * extends android.app.Activity {
   public void *(android.view.View);
}

# 保留枚举类
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# 保留Parcelable序列化的类
-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}

# 保留Serializable序列化的类
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ######################################
# # WebView 相关的混淆规则
# ######################################
-keepclassmembers class fqcn.of.javascript.interface.for.webview {
   public *;
}
-keepclassmembers class * extends android.webkit.WebChromeClient {
    public void onProgressChanged(android.webkit.WebView, int);
}

# ######################################
# # Kotlin 相关的混淆规则
# ######################################
# 保留所有注解
-keepattributes *Annotation*

# 保持 Kotlin 的元数据
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations,InnerClasses,Signature

# 保持 Kotlin 协程的相关类
-keep class kotlin.coroutines.jvm.internal.SuspendLambda {
    <fields>;
    <methods>;
}

# 保留Kotlin中所有被@JvmField注解的成员
-keepclassmembers class * {
    @kotlin.jvm.JvmField <fields>;
}

# 保留Kotlin中所有被@JvmStatic注解的成员
-keepclassmembers class * {
    @kotlin.jvm.JvmStatic <methods>;
}

-keepclassmembers class * {
    @kotlin.jvm.JvmOverloads <methods>;
}

# 保留Kotlin的伴生对象
-keepclassmembers class * {
    public static ** Companion;
}

# 解决Kotlin反射问题
-keep class kotlin.reflect.jvm.internal.** { *; }

# 解决 data class 混淆后无法正常工作的问题
-keepclassmembers class * extends kotlin.Metadata {
    public <methods>;
}

-keep class **.*$WhenMappings {
    <fields>;
    <methods>;
}

-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    public static void checkParameterIsNotNull(java.lang.Object, java.lang.String);
} 