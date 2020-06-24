rootProject.name = "producer"

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        maven(url = "https://dl.bintray.com/kotlin/kotlin-dev/")
        maven(url = "https://dl.bintray.com/kotlin/kotlin-eap/")
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.namespace == "com.android") {
                useModule("com.android.tools.build:gradle:${requested.version}")
            }
        }
    }
}

include("java-library")
include("android-library")
include("android-library-single-variant")
include("android-kotlin-library")
include("kotlin-library")
include("kotlin-multiplatform-library")
include("kotlin-multiplatform-android-library")
