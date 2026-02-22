package org.example.encryptmodule.crypto

import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties

class CryptoEntityListener {

    @PrePersist
    @PreUpdate
    fun onSave(entity: Any) {
        entity::class.memberProperties
            .filterIsInstance<KMutableProperty1<Any, Any?>>()
            .forEach { encryptedProp ->
                val plainFieldName = encryptedProp.findAnnotation<CryptoField>()?.plainField ?: return@forEach
                @Suppress("UNCHECKED_CAST")
                val plainValue = (entity::class.memberProperties.find { it.name == plainFieldName } as? KProperty1<Any, *>)
                    ?.get(entity)
                encryptedProp.set(entity, plainValue)
            }
    }
}
