plugins {
    id("java-platform")
}

group = "com.myorg.platform"

javaPlatform.allowDependencies()

dependencies {
    api(platform("org.springframework.boot:spring-boot-dependencies:2.3.3.RELEASE"))

    constraints {
        api("org.apache.juneau:juneau-marshall:8.2.0")
    }
}