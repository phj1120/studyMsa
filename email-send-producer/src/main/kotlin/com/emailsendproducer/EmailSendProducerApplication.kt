package com.emailsendproducer

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class EmailSendProducerApplication

fun main(args: Array<String>) {
	runApplication<EmailSendProducerApplication>(*args)
}
