package com.emailsendconsumer

import tools.jackson.databind.ObjectMapper

data class EmailSendMessage(
    val from: String,
    val to: String,
    val subject: String,
    val body: String,
) {
    companion object {
        fun fromJson(json: String): EmailSendMessage {
            val objectMapper = ObjectMapper()
            return objectMapper.readValue(json, EmailSendMessage::class.java)
        }
    }
}
