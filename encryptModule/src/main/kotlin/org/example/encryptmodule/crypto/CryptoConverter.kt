package org.example.encryptmodule.crypto

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = false)
class CryptoConverter : AttributeConverter<String, String> {
    override fun convertToDatabaseColumn(attribute: String?): String? {
        return attribute?.let {
            it + "encrypted"
        }
    }

    override fun convertToEntityAttribute(dbData: String?): String? {
        return dbData?.let {
            it.removeSuffix("encrypted")
        }
    }
}