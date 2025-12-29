package com.otheruncle.memorydisplay.data.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * Serializer for boolean values that may come as "0"/"1" strings or true/false from the API
 */
object StringBooleanSerializer : KSerializer<Boolean> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("StringBoolean", PrimitiveKind.BOOLEAN)

    override fun deserialize(decoder: Decoder): Boolean {
        val jsonDecoder = decoder as? JsonDecoder
            ?: return decoder.decodeBoolean()
        
        val element = jsonDecoder.decodeJsonElement().jsonPrimitive
        
        // Try as boolean first
        element.booleanOrNull?.let { return it }
        
        // Try as string
        val content = element.content
        return when (content) {
            "1", "true" -> true
            "0", "false", "" -> false
            else -> content.toBooleanStrictOrNull() ?: false
        }
    }

    override fun serialize(encoder: Encoder, value: Boolean) {
        val jsonEncoder = encoder as? JsonEncoder
        if (jsonEncoder != null) {
            jsonEncoder.encodeJsonElement(JsonPrimitive(if (value) "1" else "0"))
        } else {
            encoder.encodeBoolean(value)
        }
    }
}

/**
 * Serializer for Int values that may come as strings from the API
 */
object StringIntSerializer : KSerializer<Int> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("StringInt", PrimitiveKind.INT)

    override fun deserialize(decoder: Decoder): Int {
        val jsonDecoder = decoder as? JsonDecoder
            ?: return decoder.decodeInt()
        
        val element = jsonDecoder.decodeJsonElement().jsonPrimitive
        
        // Try as int first
        element.intOrNull?.let { return it }
        
        // Try as string
        return element.content.toIntOrNull() ?: 0
    }

    override fun serialize(encoder: Encoder, value: Int) {
        encoder.encodeInt(value)
    }
}

/**
 * Serializer for nullable Int values that may come as strings from the API
 */
object NullableStringIntSerializer : KSerializer<Int?> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("NullableStringInt", PrimitiveKind.INT)

    override fun deserialize(decoder: Decoder): Int? {
        val jsonDecoder = decoder as? JsonDecoder
            ?: return decoder.decodeInt()
        
        val element = jsonDecoder.decodeJsonElement().jsonPrimitive
        
        // Try as int first
        element.intOrNull?.let { return it }
        
        // Try as string
        return element.content.toIntOrNull()
    }

    override fun serialize(encoder: Encoder, value: Int?) {
        if (value != null) {
            encoder.encodeInt(value)
        } else {
            encoder.encodeNull()
        }
    }
}

/**
 * Serializer for attendees field that can come as:
 * - null
 * - a simple string: "Family"
 * - a JSON array: ["Mom", "Dad", "Sarah"] or [1, 2, "Grandma Jones"]
 * 
 * Always normalizes to a comma-separated string for display
 */
object AttendeesSerializer : KSerializer<String?> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Attendees", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): String? {
        val jsonDecoder = decoder as? JsonDecoder
            ?: return decoder.decodeString()
        
        return when (val element = jsonDecoder.decodeJsonElement()) {
            is JsonNull -> null
            is JsonPrimitive -> {
                val content = element.content
                if (content.isBlank()) null else content
            }
            is JsonArray -> {
                // Convert array elements to comma-separated string
                val items = element.mapNotNull { item ->
                    when (item) {
                        is JsonPrimitive -> {
                            val content = item.content
                            if (content.isNotBlank()) content else null
                        }
                        else -> null
                    }
                }
                if (items.isEmpty()) null else items.joinToString(", ")
            }
            else -> null
        }
    }

    override fun serialize(encoder: Encoder, value: String?) {
        val jsonEncoder = encoder as? JsonEncoder
        if (jsonEncoder != null) {
            if (value != null) {
                jsonEncoder.encodeJsonElement(JsonPrimitive(value))
            } else {
                jsonEncoder.encodeJsonElement(JsonNull)
            }
        } else {
            if (value != null) {
                encoder.encodeString(value)
            } else {
                encoder.encodeNull()
            }
        }
    }
}
