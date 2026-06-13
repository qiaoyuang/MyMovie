import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose.compiler)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.kotlinx.atomicfu)
    alias(libs.plugins.kotlin.cocoapods)

    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.ksp)

    alias(libs.plugins.android.library)
    // alias(libs.plugins.mockative)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xexpect-actual-classes", "-Xcontext-parameters", "-Xexplicit-backing-fields")
    }

    android {
        namespace = "com.qiaoyuang.movie.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        androidResources.enable = true
        withHostTest {
            isIncludeAndroidResources = true
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach {
        it.compilerOptions {
            freeCompilerArgs.addAll("-Xbinary=preCodegenInlineThreshold=40")
        }
    }

    cocoapods {
        summary = "The main implementation of MyMovie"
        homepage = "https://github.com/qiaoyuang/MyMovie"
        name = "ComposeApp"
        version = "1.0"
        ios.deploymentTarget = "26.0"
        framework {
            baseName = "ComposeApp"
            isStatic = true
            transitiveExport = true
        }
        // Just for unit tests
        /*pod(
            name = "MMKV",
            version = libs.versions.mmkv.origin.get(),
        )*/
        xcodeConfigurationToNativeBuildType["CUSTOM_DEBUG"] = NativeBuildType.DEBUG
        xcodeConfigurationToNativeBuildType["CUSTOM_RELEASE"] = NativeBuildType.RELEASE
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material)
            implementation(libs.compose.material3)
            implementation(libs.compose.material.icons.extended)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.components.ui.tooling.preview)
            implementation(libs.compose.animation)
            implementation(libs.compose.animation.graphics)

            implementation(libs.androidx.annotation)
            implementation(libs.androidx.lifecycle.common)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.viewmodel.compose)
            implementation(libs.androidx.lifecycle.viewmodel.savestate)
            implementation(libs.androidx.lifecycle.runtime)
            implementation(libs.androidx.lifecycle.runtime.compose)

            implementation(libs.androidx.navigation3.ui)
            implementation(libs.androidx.lifecycle.viewmodel.navigation3)
            implementation(libs.androidx.material.adaptive.navigation3)

            implementation(libs.kotlinx.coroutines)
            implementation(libs.kotlinx.serialization.json)

            implementation(libs.coil.compose)
            implementation(libs.coil.ktor3)

            implementation(libs.ktor.core)
            implementation(libs.ktor.cio)
            implementation(libs.ktor.negotiation)
            implementation(libs.ktor.json)

            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.koin.compose.navigation3)

            implementation(libs.mmkv.kotlin)

            implementation(libs.sqllin.driver)
            implementation(libs.sqllin.dsl)

            implementation(libs.mockative)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.koin.test)
            implementation(libs.mockative)
        }

        androidMain.dependencies {
            implementation(libs.compose.ui.tooling.preview)
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.appcompat)
            implementation(libs.androidx.activity.compose)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.koin.android)
            implementation(libs.ktor.okhttp)
        }

        iosMain.dependencies {
            implementation(libs.ktor.darwin)
        }
    }
}

dependencies {
    add("kspCommonMainMetadata", libs.sqllin.processor)
    androidRuntimeClasspath(libs.compose.ui.tooling)
}
