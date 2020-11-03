plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

group = "com.myorg.myproduct"

android {
    compileSdkVersion(28)
    defaultConfig {
        minSdkVersion(24)
        targetSdkVersion(28)
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation(platform("com.myorg.platform:product-platform"))
    testImplementation(platform("com.myorg.platform:test-platform"))

    implementation(kotlin("stdlib"))
}