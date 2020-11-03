plugins {
    id("java-platform")
}

group = "com.myorg.platform"

javaPlatform.allowDependencies()

dependencies {
    api(platform("org.junit:junit-bom:5.7.0"))
}