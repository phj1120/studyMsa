plugins {
    kotlin("plugin.spring")
}

dependencies {
    implementation(project(":support:common"))
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("com.nimbusds:nimbus-jose-jwt:9.37.3")
}
