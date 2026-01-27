package com.emailsendconsumer

import org.springframework.kafka.annotation.BackOff
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.annotation.RetryableTopic
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Service

@Service
class EmailSendConsumer {

    @KafkaListener(
        topics = ["email.send"],
        groupId = "email-send-group",
    )
    @RetryableTopic(
        attempts = "5",
        backOff = BackOff(delay = 1000, multiplier = 2.0),
    )
    fun consume(message: String, ack: Acknowledgment) {
        println("kafka 수신: ${message}")

        val emailSendMessage = EmailSendMessage.fromJson(message)

        if (emailSendMessage.to == "fail@test.com") {
            println("발송 실패")
            throw IllegalArgumentException("발송 실패")
        }

        // 이메일 발송 로직 3초 소요
        Thread.sleep(3000)

        println("이메일 발송 완료: ${emailSendMessage}")
        ack.acknowledge()
    }
}

//kafka-topics --bootstrap-server localhost:9092 --delete --topic email.send
//kafka-topics --bootstrap-server localhost:9092 --delete --topic email.send-dlt
//kafka-topics --bootstrap-server localhost:9092 --delete --topic email.send-retry-1000
//kafka-topics --bootstrap-server localhost:9092 --delete --topic email.send-retry-2000
//kafka-topics --bootstrap-server localhost:9092 --delete --topic email.send-retry-4000
//kafka-topics --bootstrap-server localhost:9092 --delete --topic email.send-retry-8000
//kafka-topics --bootstrap-server localhost:9092 --delete --topic __consumer_offsets
//
//kafka-consumer-groups --bootstrap-server localhost:9092 --delete --group email-send-group
