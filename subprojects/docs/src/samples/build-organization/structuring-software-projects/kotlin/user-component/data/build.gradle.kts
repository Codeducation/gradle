plugins {
    id("com.myorg.java-library")
}

group = "${group}.user-component"

dependencies {
    api("com.myorg.myproduct.model:release")

    implementation("com.fasterxml.jackson.core:jackson-databind")
}
