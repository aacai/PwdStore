rootProject.name = "PwdStore"

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include(":androidApp")
include(":desktopApp")
include(":shared")

// okui 子模块中的组件库与 oink 主题（oink 依赖同模块下的 applewidgets-ui）
include(":applewidgets-ui")
project(":applewidgets-ui").projectDir = file("okui/applewidgets-ui")
include(":oink")
project(":oink").projectDir = file("okui/oink")