package com.emailsendconsumer

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class EmailSendConsumerApplication

fun main(args: Array<String>) {
    runApplication<EmailSendConsumerApplication>(*args)
}
