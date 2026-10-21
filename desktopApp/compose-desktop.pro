# Room 通过反射实例化 KSP 生成的 *Impl 数据库类
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep class zhiqiu.app.db.PwdDatabase { *; }
-keep class zhiqiu.app.db.PwdDatabaseConstructor { *; }
-keep class zhiqiu.app.db.PasswordEntryEntity { *; }
-keep class zhiqiu.app.db.MetaEntity { *; }

# BundledSQLiteDriver 的 native 方法按 JNI 符号名绑定，改名后 libsqlite 加载即失败
-keep class androidx.sqlite.driver.bundled.** { *; }

# kotlinx-serialization 生成的 serializer 与 Companion 会被反射查找
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class ** {
    *** Companion;
}
-keepclasseswithmembers class ** {
    kotlinx.serialization.KSerializer serializer(...);
}
