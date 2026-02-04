package org.example.encryptmodule.domain

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id

@Entity
class Member(

    val name: String,

    val phoneNumber: String,

    val age: Int
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    var nameEncrypted: String? = null

    var phoneNumberEncrypted: String? = null
}