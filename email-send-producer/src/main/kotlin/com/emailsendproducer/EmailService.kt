package com.emailsendproducer

import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@Service
class EmailService(
    val kafkaTemplate: KafkaTemplate<String, String>
) {

    fun sendEmail(request: SendEmailRequestDto) {
        val emailSendMessage = EmailSendMessage(
            request.from, request.to, request.subject, request.body
        )

        this.kafkaTemplate.send("email.send", toJsonString(emailSendMessage))
    }

}