package org.example.encryptmodule.domain

import jakarta.persistence.Convert
import jakarta.persistence.Converter
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import org.example.encryptmodule.crypto.CryptoConverter
import org.example.encryptmodule.crypto.CryptoField
import org.example.encryptmodule.crypto.CryptoUtils

@Entity
class Member(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @CryptoField(plainField = "name")
    var nameEncrypted: String? = null,

    @Convert(converter = CryptoConverter::class)
    var phoneNumberEncrypted: String? = null,

    var name: String,

    var phoneNumber: String,

    var age: Int
) {

}