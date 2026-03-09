apply(plugin = "org.jetbrains.kotlin.plugin.jpa")
apply(plugin = "java-library")

dependencies {
    implementation(project(":support:common"))
    // api: 이 모듈에 의존하는 business 모듈에서도 JPA 타입 접근 가능
    api("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    runtimeOnly("com.mysql:mysql-connector-j")
    runtimeOnly("com.h2database:h2")
}
