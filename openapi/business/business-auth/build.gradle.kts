plugins {
    kotlin("plugin.spring")
}

dependencies {
    implementation(project(":support:common"))
    implementation(project(":storages:db-core"))
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.security:spring-security-crypto")
}
