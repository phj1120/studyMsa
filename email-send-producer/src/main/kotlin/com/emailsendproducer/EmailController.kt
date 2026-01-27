package com.emailsendproducer

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/emails")
class EmailController (
    val emailService: EmailService
){

    @PostMapping
    fun sendEmail(
        @RequestBody request: SendEmailRequestDto
    ): ResponseEntity<String>{
        emailService.sendEmail(request)
        return ResponseEntity.ok("ok")
    }
}