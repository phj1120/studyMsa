package com.emailsendproducer

import tools.jackson.databind.ObjectMapper

fun toJsonString(obj: Any): String {
    val objectMapper = ObjectMapper()
    return objectMapper.writeValueAsString(obj)
}