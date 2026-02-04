package org.example.encryptmodule.v2

import org.w3c.dom.Text

class CryptoUtils {
    companion object {
        fun encrypt(plainText: String): String {
            return "${plainText}-encrypt"
        }

        fun decrypt(encryptText: String): String {
            return encryptText.split("-")[0]
        }

        fun getDecryptText(plainText: String, encryptText: String?): String {
            return if (encryptText == null) plainText else decrypt(encryptText)
        }
    }
}