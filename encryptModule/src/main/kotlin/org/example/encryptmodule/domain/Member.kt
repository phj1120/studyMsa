package org.example.encryptmodule.domain

import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import org.example.encryptmodule.crypto.CryptoConverter
import org.example.encryptmodule.crypto.CryptoField
import org.example.encryptmodule.crypto.CryptoUtils
import org.example.encryptmodule.crypto.CryptoEntityListener

@Entity
@EntityListeners(CryptoEntityListener::class)
class Member(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @CryptoField(plainField = "name")
    @Convert(converter = CryptoConverter::class)
    var nameEncrypted: String? = null,

    @CryptoField(plainField = "phoneNumber")
    @Convert(converter = CryptoConverter::class)
    var phoneNumberEncrypted: String? = null,

    var name: String,

    var phoneNumber: String,

    var age: Int
) {

}