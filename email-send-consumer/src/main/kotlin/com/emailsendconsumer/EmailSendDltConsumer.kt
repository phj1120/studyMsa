package com.emailsendconsumer

import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service

@Service
class EmailSendDltConsumer {

    @KafkaListener(
        topics = ["email.send-dlt"],
        groupId = "email-send-dlt-group"
    )
    fun consume(message: String) {
        println("DLT Topic ${message}")
    }
}