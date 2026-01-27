package com.emailsendproducer

data class EmailSendMessage (
    val from: String,
    val to: String,
    val subject: String,
    val body: String,
)