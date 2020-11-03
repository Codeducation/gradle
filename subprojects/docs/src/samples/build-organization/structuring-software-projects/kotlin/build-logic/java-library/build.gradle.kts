plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(platform("com.myorg.platform:plugins-platform"))

    implementation(project(":commons"))
}
