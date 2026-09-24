import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}
dependencies {
    implementation(project(":shared"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.fragment)

    debugImplementation(libs.compose.uiTooling)
}

/**
 * 签名来源优先级：
 * 1) CI / 环境变量：KEYSTORE_FILE、KEYSTORE_PASSWORD、KEY_ALIAS、KEY_PASSWORD
 * 2) 仓库根目录本地文件：key.jks + keyconf.txt
 */
fun loadLocalKeyConf(): Map<String, String> {
    val conf = rootProject.file("keyconf.txt")
    if (!conf.exists()) return emptyMap()
    val map = mutableMapOf<String, String>()
    conf.readLines().forEach { raw ->
        val line = raw.trim().removePrefix("-").trim()
        val sep = when {
            "：" in line -> "："
            ":" in line -> ":"
            else -> return@forEach
        }
        val key = line.substringBefore(sep).trim()
        val value = line.substringAfter(sep).trim()
        when {
            key.contains("密钥库密码") || key.equals("storePassword", true) -> map["storePassword"] = value
            key.contains("密钥别名") || key.equals("keyAlias", true) -> map["keyAlias"] = value
            key.contains("密钥密码") || key.equals("keyPassword", true) -> map["keyPassword"] = value
        }
    }
    return map
}

val localKeyConf = loadLocalKeyConf()
val releaseKeystorePath = System.getenv("KEYSTORE_FILE")
    ?.takeIf { it.isNotBlank() }
    ?: rootProject.file("key.jks").takeIf { it.isFile }?.absolutePath
val releaseStorePassword = System.getenv("KEYSTORE_PASSWORD")
    ?.takeIf { it.isNotBlank() }
    ?: localKeyConf["storePassword"]
val releaseKeyAlias = System.getenv("KEY_ALIAS")
    ?.takeIf { it.isNotBlank() }
    ?: localKeyConf["keyAlias"]
val releaseKeyPassword = System.getenv("KEY_PASSWORD")
    ?.takeIf { it.isNotBlank() }
    ?: localKeyConf["keyPassword"]
val canSignRelease = !releaseKeystorePath.isNullOrBlank() &&
    !releaseStorePassword.isNullOrBlank() &&
    !releaseKeyAlias.isNullOrBlank() &&
    !releaseKeyPassword.isNullOrBlank() &&
    file(releaseKeystorePath!!).isFile

android {
    namespace = "zhiqiu.app"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "zhiqiu.app"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
        ndk {
            abiFilters += listOf("arm64-v8a")
        }
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    signingConfigs {
        if (canSignRelease) {
            create("release") {
                storeFile = file(releaseKeystorePath!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (canSignRelease) {
                signingConfig = signingConfigs.getByName("release")
            } else {
                logger.warn(
                    "Release 未配置签名：请放置 key.jks + keyconf.txt，或设置 KEYSTORE_* 环境变量",
                )
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}
