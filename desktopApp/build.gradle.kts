import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    implementation(project(":shared"))

    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)

    implementation(libs.compose.uiToolingPreview)
}

compose.desktop {
    application {
        mainClass = "zhiqiu.app.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Exe, TargetFormat.Deb)
            packageName = "PwdStore"
            packageVersion = libs.versions.app.get()

            // deb 包名必须小写，显式声明避免依赖打包器的隐式规则
            linux {
                packageName = "pwdstore"
            }
            // bundleID 决定 macOS 上的应用身份（LaunchServices / 权限），改名后保持历史值
            macOS {
                bundleID = "zhiqiu.app"
            }
        }
    }
}