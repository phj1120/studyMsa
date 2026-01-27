package com.emailsendproducer

data class SendEmailRequestDto (
    val from: String,
    val to: String,
    val subject: String,
    val body: String
)