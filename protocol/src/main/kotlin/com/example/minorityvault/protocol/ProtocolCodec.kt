package com.example.minorityvault.protocol

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** What goes over the air: `{ v, seq, type, payload }` (CLAUDE.md §7). */
@Serializable
data class Envelope(val v: String, val seq: Long, val type: String, val payload: JsonObject)

sealed interface Decoded<out T> {
    data class Message<T>(val seq: Long, val message: T) : Decoded<T>

    /** The sender speaks another major version. Nothing else in the envelope can be trusted. */
    data class VersionMismatch(val theirVersion: String) : Decoded<Nothing>

    /** Not JSON, not an envelope, or a type this build doesn't know. */
    data class Malformed(val reason: String) : Decoded<Nothing>
}

/** Turns messages into envelope bytes and back. Decoding never throws. */
class ProtocolCodec(val version: String = ProtocolVersion.CURRENT) {
    fun encode(seq: Long, message: ClientIntent): ByteArray = encode(seq, message, ClientIntent.serializer())

    fun encode(seq: Long, message: HostEvent): ByteArray = encode(seq, message, HostEvent.serializer())

    fun decodeClientIntent(bytes: ByteArray): Decoded<ClientIntent> = decode(bytes, ClientIntent.serializer())

    fun decodeHostEvent(bytes: ByteArray): Decoded<HostEvent> = decode(bytes, HostEvent.serializer())

    private fun <T> encode(seq: Long, message: T, serializer: KSerializer<T>): ByteArray {
        // The serializer writes the subtype into a "type" field; it moves up into the envelope.
        val tagged = json.encodeToJsonElement(serializer, message).jsonObject
        val type = tagged.getValue(TYPE_KEY).jsonPrimitive.content
        val envelope = Envelope(version, seq, type, JsonObject(tagged - TYPE_KEY))
        return json.encodeToString(Envelope.serializer(), envelope).encodeToByteArray()
    }

    private fun <T> decode(bytes: ByteArray, serializer: KSerializer<T>): Decoded<T> =
        attempt { json.decodeFromString(Envelope.serializer(), bytes.decodeToString()) }.fold(
            onSuccess = { decodePayload(it, serializer) },
            onFailure = { Decoded.Malformed("not an envelope: ${it.message}") },
        )

    private fun <T> decodePayload(envelope: Envelope, serializer: KSerializer<T>): Decoded<T> {
        if (ProtocolVersion.majorOf(envelope.v) != ProtocolVersion.majorOf(version)) {
            return Decoded.VersionMismatch(envelope.v)
        }
        val tagged = JsonObject(envelope.payload + (TYPE_KEY to JsonPrimitive(envelope.type)))
        return attempt { json.decodeFromJsonElement(serializer, tagged) }.fold(
            onSuccess = { Decoded.Message(envelope.seq, it) },
            onFailure = { Decoded.Malformed("bad ${envelope.type}: ${it.message}") },
        )
    }

    /** kotlinx.serialization reports bad input as IllegalArgumentException (SerializationException included). */
    private inline fun <T> attempt(block: () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: IllegalArgumentException) {
        Result.failure(e)
    }

    private companion object {
        const val TYPE_KEY = "type"

        val json = Json {
            // A newer minor version may add fields; older phones skip them.
            ignoreUnknownKeys = true
            explicitNulls = false
            classDiscriminator = TYPE_KEY
        }
    }
}
