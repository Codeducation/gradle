// == Define locations for build logic ==
pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
    }
}
includeBuild("../platforms")
includeBuild("../build-logic")

// == Define locations for components ==
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
        jcenter()
    }
}
includeBuild("../platforms")
includeBuild("../user-component")
includeBuild("../state")              // TODO because nested are not shown in IDEA
includeBuild("../domain-model")       // TODO because nested are not shown in IDEA

// == Define the inner structure of this component ==
rootProject.name = "android-app"
include("app")
