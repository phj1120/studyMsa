plugins {
    kotlin("plugin.spring")
}

dependencies {
    implementation(project(":support:common"))
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
}
