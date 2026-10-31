package com.example.minorityvault.protocol

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.elementNames
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.junit.jupiter.api.Test

class CodecTest {
    private val codec = ProtocolCodec()

    /** The serial names of every subtype of a sealed serializer. */
    private val SerialDescriptor.subtypeNames get() = getElementDescriptor(1).elementNames.toList()

    @Test
    fun `the samples cover every message type`() {
        clientSamples.map { codec.typeOf(codec.encode(0, it)) }.distinct() shouldContainExactlyInAnyOrder
            ClientIntent.serializer().descriptor.subtypeNames
        hostSamples.map { codec.typeOf(codec.encode(0, it)) }.distinct() shouldContainExactlyInAnyOrder
            HostEvent.serializer().descriptor.subtypeNames
    }

    @Test
    fun `every client message survives the trip`() {
        clientSamples.forEachIndexed { index, message ->
            val seq = index.toLong()
            codec.decodeClientIntent(codec.encode(seq, message)) shouldBe Decoded.Message(seq, message)
        }
    }

    @Test
    fun `every host message survives the trip`() {
        hostSamples.forEachIndexed { index, message ->
            val seq = index.toLong()
            codec.decodeHostEvent(codec.encode(seq, message)) shouldBe Decoded.Message(seq, message)
        }
    }

    @Test
    fun `the envelope is v, seq, type and payload`() {
        val envelope = Json.parseToJsonElement(codec.encode(9, ClientIntent.Join("Asha")).decodeToString()).jsonObject

        envelope.keys shouldBe setOf("v", "seq", "type", "payload")
        envelope.getValue("v").jsonPrimitive.content shouldBe ProtocolVersion.CURRENT
        envelope.getValue("seq").jsonPrimitive.long shouldBe 9
        envelope.getValue("type").jsonPrimitive.content shouldBe "Join"
        envelope.getValue("payload").jsonObject.keys shouldBe setOf("name")
    }

    @Test
    fun `a different major version is refused before reading the payload`() {
        val newer = ProtocolCodec(version = "${ProtocolVersion.MAJOR + 1}.0")

        codec.decodeHostEvent(newer.encode(1, HostEvent.KeeperLine("hi"))) shouldBe
            Decoded.VersionMismatch("${ProtocolVersion.MAJOR + 1}.0")
    }

    @Test
    fun `a newer minor version with extra fields still decodes`() {
        val bytes = """{"v":"${ProtocolVersion.MAJOR}.9","seq":3,"type":"KeeperLine",
            "payload":{"text":"hi","mood":"smug"},"sentFrom":"the future"}""".encodeToByteArray()

        codec.decodeHostEvent(bytes) shouldBe Decoded.Message(3, HostEvent.KeeperLine("hi"))
    }

    @Test
    fun `garbage never throws`() {
        val garbage = listOf(
            "not json",
            "{}",
            """{"v":"1.0","seq":1,"type":"Join"}""",
            """{"v":"1.0","seq":1,"type":"Teleport","payload":{}}""",
            """{"v":"1.0","seq":1,"type":"Join","payload":{"name":42}}""",
            """{"v":"1.0","seq":1,"type":"Join","payload":{}}""",
            """{"v":"banana","seq":1,"type":"Join","payload":{"name":"x"}}""",
        )
        garbage.forEach { text ->
            val decoded = codec.decodeClientIntent(text.encodeToByteArray())
            if (decoded !is Decoded.VersionMismatch) decoded.shouldBeInstanceOf<Decoded.Malformed>()
        }
    }

    @Test
    fun `host messages are not client messages`() {
        codec.decodeClientIntent(codec.encode(1, HostEvent.KeeperLine("hi"))).shouldBeInstanceOf<Decoded.Malformed>()
    }

    private fun ProtocolCodec.typeOf(bytes: ByteArray): String =
        Json.parseToJsonElement(bytes.decodeToString()).jsonObject.getValue("type").jsonPrimitive.content
}
