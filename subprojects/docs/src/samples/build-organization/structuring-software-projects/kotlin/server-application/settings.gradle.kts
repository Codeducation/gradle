// == Define locations for build logic ==
pluginManagement {
    repositories {
        gradlePluginPortal()
    }
}
includeBuild("../platforms")
includeBuild("../build-logic")

// == Define locations for components ==
dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}
includeBuild("../user-component")
includeBuild("../admin-component")

// == Define the inner structure of this component ==
rootProject.name = "server-application" // the component name
include("app")
