package org.example.openapi.admin

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@SpringBootApplication(
    scanBasePackages = [
        "org.example.openapi.admin",
        "org.example.openapi.auth",
        "org.example.openapi.webapi",
        "org.example.openapi.db",
    ],
)
@EnableJpaRepositories(basePackages = ["org.example.openapi.db.repository"])
@EntityScan(basePackages = ["org.example.openapi.db.entity"])
class TenantAdminApplication

fun main(args: Array<String>) {
    runApplication<TenantAdminApplication>(*args)
}
