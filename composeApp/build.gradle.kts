import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlinx.serialization)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xwhen-guards", "-Xnon-local-break-continue", "-Xmulti-dollar-interpolation", /*"-Xbinary=preCodegenInlineThreshold=40"*/)
    }

    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
    
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }
    
    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(compose.animation)
            implementation(compose.animationGraphics)

            implementation(libs.androidx.annotation)
            implementation(libs.androidx.lifecycle.common)
            implementation(libs.androidx.lifecycle.viewmodel)
            api(libs.androidx.lifecycle.viewmodel.compose)
            implementation(libs.androidx.lifecycle.viewmodel.savestate)
            implementation(libs.androidx.lifecycle.runtime)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.androidx.navigation)

            implementation(libs.kotlinx.coroutines)
            implementation(libs.kotlinx.serialization.json)

            implementation(libs.coil.compose)
            implementation(libs.coil.ktor3)

            implementation(libs.ktor.core)
            // implementation(libs.ktor.cio)
            implementation(libs.ktor.negotiation)
            implementation(libs.ktor.json)

            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.koin.compose.viewmodel.navigation)
        }

        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.koin.android)
            implementation(libs.ktor.okhttp)
        }

        iosMain.dependencies {
            implementation(libs.ktor.darwin)
        }

        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.koin.test)
        }
    }
}

android {
    namespace = "com.qiaoyuang.movie"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].res.srcDirs("src/androidMain/res")
    sourceSets["main"].resources.srcDirs("src/commonMain/resources")

    /*androidResources {
        generateLocaleConfig = true
    }*/
    defaultConfig {
        applicationId = "com.qiaoyuang.movie"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
    dependencies {
        debugImplementation(compose.uiTooling)
    }
}
