import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.services)
}


android {
    namespace = "com.mesawa.cuidarproximocuidador"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mesawa.cuidarproximocuidador"
        minSdk = 25
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }
}

configurations.all {
    resolutionStrategy.force("androidx.core:core-ktx:1.13.0")
    resolutionStrategy.force("androidx.lifecycle:lifecycle-common:2.8.7")
    resolutionStrategy.force("androidx.lifecycle:lifecycle-viewmodel:2.8.7")
    resolutionStrategy.force("androidx.lifecycle:lifecycle-viewmodel-savedstate:2.8.7")
    resolutionStrategy.force("androidx.lifecycle:lifecycle-livedata-core:2.8.7")
    resolutionStrategy.force("androidx.lifecycle:lifecycle-runtime:2.6.2")
    resolutionStrategy.force("androidx.savedstate:savedstate:1.3.2")
    resolutionStrategy.force("androidx.annotation:annotation-experimental:1.4.1")
}

dependencies {
    implementation(platform(libs.firebase.bom))
    implementation(libs.androidx.activity)
    implementation(libs.androidx.core)
    implementation("com.google.android.material:material:1.14.0")
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.livedata)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.play.services.auth)
    implementation(libs.play.app.update)
    implementation(libs.play.app.update.ktx)
    implementation(libs.play.app.update)
    implementation(libs.play.app.update.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
