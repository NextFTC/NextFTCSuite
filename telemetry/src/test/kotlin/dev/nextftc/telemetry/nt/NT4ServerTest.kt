/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.telemetry.nt

import dev.nextftc.telemetry.struct.structSchema
import dev.nextftc.telemetry.wpilog.WPILogWriter
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.websocket.Frame
import io.ktor.websocket.readBytes
import io.ktor.websocket.readText
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.msgpack.core.MessagePack
import org.msgpack.value.Value
import org.msgpack.value.ValueType
import java.io.ByteArrayOutputStream
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource

// region Helpers

internal fun Value.toKotlin(): Any? = when (valueType) {
  ValueType.INTEGER -> asIntegerValue().toLong()
  ValueType.FLOAT -> asFloatValue().toDouble()
  ValueType.STRING -> asStringValue().asString()
  ValueType.BINARY -> asBinaryValue().asByteArray()
  ValueType.BOOLEAN -> asBooleanValue().boolean
  ValueType.ARRAY -> asArrayValue().list().map { it.toKotlin() }
  else -> null
}

/** Decodes one MessagePack message, a 4-element array, into its elements. */
internal fun decode(bytes: ByteArray): List<Any?> =
  MessagePack.newDefaultUnpacker(bytes).unpackValue().asArrayValue().list().map { it.toKotlin() }

internal fun subscribeMessage(
  subscriptionId: Int,
  vararg topics: String,
  prefix: Boolean = false,
  topicsOnly: Boolean = false,
): String = buildJsonArray {
  add(
    buildJsonObject {
      put("method", "subscribe")
      put(
        "params",
        buildJsonObject {
          put("topics", buildJsonArray { topics.forEach { add(it) } })
          put("subuid", subscriptionId)
          put(
            "options",
            buildJsonObject {
              put("prefix", prefix)
              put("topicsonly", topicsOnly)
            },
          )
        },
      )
    },
  )
}
  .toString()

internal fun unsubscribeMessage(subscriptionId: Int): String = buildJsonArray {
  add(
    buildJsonObject {
      put("method", "unsubscribe")
      put("params", buildJsonObject { put("subuid", subscriptionId) })
    },
  )
}
  .toString()

/** An RTT message from a client: id -1, timestamp 0, type int, and the client's time. */
internal fun rttMessage(clientTime: Long): ByteArray {
  val packer = MessagePack.newDefaultBufferPacker()
  packer.packArrayHeader(4)
  packer.packLong(-1)
  packer.packLong(0)
  packer.packLong(2)
  packer.packLong(clientTime)
  return packer.toByteArray()
}

internal suspend fun DefaultClientWebSocketSession.nextFrame(): Frame =
  withTimeout(5.seconds) { incoming.receive() }

/** The next frame as JSON control messages. */
internal suspend fun DefaultClientWebSocketSession.nextMessages(): JsonArray =
  Json.parseToJsonElement((nextFrame() as Frame.Text).readText()).jsonArray

/** The next frame as an announce message's parameters. */
internal suspend fun DefaultClientWebSocketSession.nextAnnounce(): JsonObject {
  val message = nextMessages().single().jsonObject
  message["method"]!!.jsonPrimitive.content shouldBe "announce"
  return message["params"]!!.jsonObject
}

/** The next frame as a value update: topic id, timestamp, type id, and value. */
internal suspend fun DefaultClientWebSocketSession.nextValue(): List<Any?> =
  decode((nextFrame() as Frame.Binary).readBytes())

internal suspend fun DefaultClientWebSocketSession.expectNoFrame() {
  withTimeoutOrNull(300.milliseconds) { incoming.receive() } shouldBe null
}

internal suspend fun DefaultClientWebSocketSession.sendText(text: String) = send(Frame.Text(text))

internal suspend fun DefaultClientWebSocketSession.sendBinary(bytes: ByteArray) =
  send(Frame.Binary(true, bytes))

internal fun JsonObject.string(key: String): String = this[key]!!.jsonPrimitive.content

internal suspend fun withServer(
  timeSource: kotlin.time.TimeSource.WithComparableMarks = kotlin.time.TimeSource.Monotonic,
  block: suspend (server: NT4Server, http: HttpClient) -> Unit,
) {
  val server = NT4Server(port = 0, host = "127.0.0.1", timeSource = timeSource)
  server.start()
  val http = HttpClient(CIO) { install(WebSockets) }
  try {
    block(server, http)
  } finally {
    http.close()
    server.stop()
  }
}

internal suspend fun HttpClient.connect(
  server: NT4Server,
  protocol: String = NT4Server.PROTOCOL_V4_1,
  block: suspend DefaultClientWebSocketSession.() -> Unit,
) = webSocket(
  urlString = "ws://127.0.0.1:${server.boundPort}/nt/test",
  request = { header(HttpHeaders.SecWebSocketProtocol, protocol) },
  block = block,
)

// endregion

class NT4ServerTest :
  FunSpec({
    test("a subscriber is announced the topic and sent its current value") {
      withServer { server, http ->
        val id = server.publish("/Robot/Battery", NT4Type.DOUBLE)
        server.setDouble(id, 12.5)

        http.connect(server) {
          sendText(subscribeMessage(1, "/", prefix = true))

          val announce = nextAnnounce()
          announce.string("name") shouldBe "/Robot/Battery"
          announce.string("type") shouldBe "double"
          announce.string("id") shouldBe id.toString()
          announce["properties"]!!.jsonObject.size shouldBe 0
          val value = nextValue()
          value[0] shouldBe id.toLong()
          value[2] shouldBe 1L
          value[3] shouldBe 12.5
        }
      }
    }

    test("a topic's properties are announced to its subscribers, alongside retained") {
      withServer { server, http ->
        server.publish(
          "/Robot/Speed",
          NT4Type.DOUBLE,
          retained = true,
          properties = mapOf("unit" to "m/s", "limit" to 2.5, "tuned" to false),
        )

        http.connect(server) {
          sendText(subscribeMessage(1, "/", prefix = true))

          val properties = nextAnnounce()["properties"]!!.jsonObject
          properties.string("unit") shouldBe "m/s"
          properties.string("limit") shouldBe "2.5"
          properties.string("tuned") shouldBe "false"
          properties.string("retained") shouldBe "true"
        }
      }
    }

    test(
      "a property that isn't a string, boolean, or finite number is refused before anything is published",
    ) {
      val server = NT4Server(port = 0)

      shouldThrow<IllegalArgumentException> {
        server.publish("/a", NT4Type.DOUBLE, properties = mapOf("unit" to listOf("m")))
      }
      shouldThrow<IllegalArgumentException> {
        server.publish("/a", NT4Type.DOUBLE, properties = mapOf("limit" to Double.NaN))
      }

      // The failed attempts left no trace, so the name is still free for any type.
      server.publish("/a", NT4Type.STRING)
    }

    test("later changes are sent as they happen, in order") {
      withServer { server, http ->
        val id = server.publish("/a", NT4Type.DOUBLE)

        http.connect(server) {
          sendText(subscribeMessage(1, "/a"))
          nextAnnounce()

          for (i in 1..5) server.setDouble(id, i.toDouble())
          (1..5).map { nextValue()[3] } shouldContainExactly (1..5).map { it.toDouble() }
        }
      }
    }

    test("a client connecting late is sent only the latest value") {
      withServer { server, http ->
        val id = server.publish("/a", NT4Type.DOUBLE)
        for (i in 1..3) server.setDouble(id, i.toDouble())

        http.connect(server) {
          sendText(subscribeMessage(1, "/a"))
          nextAnnounce()

          nextValue()[3] shouldBe 3.0
          expectNoFrame()
        }
      }
    }

    test("a topic published after the subscription is announced and then sent") {
      withServer { server, http ->
        http.connect(server) {
          sendText(subscribeMessage(1, "/Robot/", prefix = true))
          expectNoFrame()

          val id = server.publish("/Robot/Heading", NT4Type.DOUBLE)
          server.setDouble(id, 0.5)

          nextAnnounce().string("name") shouldBe "/Robot/Heading"
          nextValue()[3] shouldBe 0.5
        }
      }
    }

    test("topics that don't match the subscription are not announced") {
      withServer { server, http ->
        server.publish("/a", NT4Type.DOUBLE)
        server.publish("/b", NT4Type.DOUBLE)

        http.connect(server) {
          sendText(subscribeMessage(1, "/b"))

          nextAnnounce().string("name") shouldBe "/b"
          expectNoFrame()
        }
      }
    }

    test("a prefix only matches when the prefix option is set") {
      withServer { server, http ->
        server.publish("/Robot/Heading", NT4Type.DOUBLE)

        http.connect(server) {
          sendText(subscribeMessage(1, "/Robot"))
          expectNoFrame()

          sendText(subscribeMessage(2, "/Robot", prefix = true))
          nextAnnounce().string("name") shouldBe "/Robot/Heading"
        }
      }
    }

    test("a topics-only subscription is announced but sent no values") {
      withServer { server, http ->
        val id = server.publish("/a", NT4Type.DOUBLE)
        server.setDouble(id, 1.0)

        http.connect(server) {
          sendText(subscribeMessage(1, "/", prefix = true, topicsOnly = true))
          nextAnnounce().string("name") shouldBe "/a"
          expectNoFrame()

          server.setDouble(id, 2.0)
          expectNoFrame()
        }
      }
    }

    test("subscribing for values after topics only sends the current value without re-announcing") {
      withServer { server, http ->
        val id = server.publish("/a", NT4Type.DOUBLE)
        server.setDouble(id, 7.0)

        http.connect(server) {
          sendText(subscribeMessage(1, "/", prefix = true, topicsOnly = true))
          nextAnnounce()

          sendText(subscribeMessage(2, "/a"))
          nextValue()[3] shouldBe 7.0
          expectNoFrame()
        }
      }
    }

    test("overlapping subscriptions send each change once") {
      withServer { server, http ->
        val id = server.publish("/a", NT4Type.DOUBLE)

        http.connect(server) {
          sendText(subscribeMessage(1, "/", prefix = true))
          sendText(subscribeMessage(2, "/a"))
          nextAnnounce()

          server.setDouble(id, 1.0)
          nextValue()[3] shouldBe 1.0
          expectNoFrame()
        }
      }
    }

    test("subscribing again with the same id replaces the subscription") {
      withServer { server, http ->
        server.publish("/a", NT4Type.DOUBLE)
        server.publish("/b", NT4Type.DOUBLE)

        http.connect(server) {
          sendText(subscribeMessage(1, "/a"))
          nextAnnounce().string("name") shouldBe "/a"

          sendText(subscribeMessage(1, "/b"))
          nextAnnounce().string("name") shouldBe "/b"
        }
      }
    }

    test("an unsubscribed topic stops receiving values") {
      withServer { server, http ->
        val id = server.publish("/a", NT4Type.DOUBLE)

        http.connect(server) {
          sendText(subscribeMessage(1, "/a"))
          nextAnnounce()
          server.setDouble(id, 1.0)
          nextValue()

          sendText(unsubscribeMessage(1))
          // Frames are handled in order, so the reply to this means the unsubscribe has taken effect.
          sendBinary(rttMessage(1))
          nextValue()[0] shouldBe -1L
          server.setDouble(id, 2.0)
          expectNoFrame()
        }
      }
    }

    test("clients are independent of each other") {
      withServer { server, http ->
        val id = server.publish("/a", NT4Type.DOUBLE)

        http.connect(server) {
          sendText(subscribeMessage(1, "/a"))
          nextAnnounce()
          http.connect(server) {
            sendText(subscribeMessage(1, "/a", topicsOnly = true))
            nextAnnounce()
            expectNoFrame()

            server.setDouble(id, 3.0)
            expectNoFrame()
          }
          nextValue()[3] shouldBe 3.0
        }
      }
    }

    test("every value type is encoded for its type id") {
      withServer { server, http ->
        val boolean = server.publish("/boolean", NT4Type.BOOLEAN)
        val integer = server.publish("/integer", NT4Type.INTEGER)
        val float = server.publish("/float", NT4Type.FLOAT)
        val string = server.publish("/string", NT4Type.STRING)
        val raw = server.publish("/raw", NT4Type.RAW)
        val booleans = server.publish("/booleans", NT4Type.BOOLEAN_ARRAY)
        val integers = server.publish("/integers", NT4Type.INTEGER_ARRAY)
        val floats = server.publish("/floats", NT4Type.FLOAT_ARRAY)
        val doubles = server.publish("/doubles", NT4Type.DOUBLE_ARRAY)
        val strings = server.publish("/strings", NT4Type.STRING_ARRAY)
        server.setBoolean(boolean, true)
        server.setInteger(integer, -5)
        server.setFloat(float, 1.5f)
        server.setString(string, "héllo")
        server.setRaw(raw, byteArrayOf(1, 2, 3))
        server.setBooleanArray(booleans, booleanArrayOf(true, false))
        server.setIntegerArray(integers, longArrayOf(1, -2))
        server.setFloatArray(floats, floatArrayOf(0.5f))
        server.setDoubleArray(doubles, doubleArrayOf(0.25, 4.0))
        server.setStringArray(strings, listOf("a", "bc"))

        http.connect(server) {
          sendText(subscribeMessage(1, "/", prefix = true))
          val values = HashMap<Long, List<Any?>>()
          repeat(10) {
            nextAnnounce()
            val value = nextValue()
            values[value[0] as Long] = value
          }

          values[boolean.toLong()]!![2] shouldBe 0L
          values[boolean.toLong()]!![3] shouldBe true
          values[integer.toLong()]!![2] shouldBe 2L
          values[integer.toLong()]!![3] shouldBe -5L
          values[float.toLong()]!![2] shouldBe 3L
          values[float.toLong()]!![3] shouldBe 1.5
          values[string.toLong()]!![2] shouldBe 4L
          values[string.toLong()]!![3] shouldBe "héllo"
          values[raw.toLong()]!![2] shouldBe 5L
          (values[raw.toLong()]!![3] as ByteArray).toList() shouldBe listOf<Byte>(1, 2, 3)
          values[booleans.toLong()]!![2] shouldBe 16L
          values[booleans.toLong()]!![3] shouldBe listOf(true, false)
          values[integers.toLong()]!![2] shouldBe 18L
          values[integers.toLong()]!![3] shouldBe listOf(1L, -2L)
          values[floats.toLong()]!![2] shouldBe 19L
          values[floats.toLong()]!![3] shouldBe listOf(0.5)
          values[doubles.toLong()]!![2] shouldBe 17L
          values[doubles.toLong()]!![3] shouldBe listOf(0.25, 4.0)
          values[strings.toLong()]!![2] shouldBe 20L
          values[strings.toLong()]!![3] shouldBe listOf("a", "bc")
        }
      }
    }

    test("a retained topic announces the retained property") {
      withServer { server, http ->
        server.publish("/a", NT4Type.DOUBLE, retained = true)

        http.connect(server) {
          sendText(subscribeMessage(1, "/a"))

          nextAnnounce()["properties"]!!.jsonObject.string("retained") shouldBe "true"
        }
      }
    }

    test("unpublishing a topic tells the clients that knew about it") {
      withServer { server, http ->
        val id = server.publish("/a", NT4Type.DOUBLE)

        http.connect(server) {
          sendText(subscribeMessage(1, "/a"))
          nextAnnounce()

          server.unpublish(id)
          val message = nextMessages().single().jsonObject
          message.string("method") shouldBe "unannounce"
          message["params"]!!.jsonObject.string("name") shouldBe "/a"
          message["params"]!!.jsonObject.string("id") shouldBe id.toString()
        }
      }
    }

    test("a struct topic announces its schema topics first, each with its schema as the value") {
      val translation =
        structSchema<Pair<Double, Double>>("Translation2d") {
          float64("x") { it.first }
          float64("y") { it.second }
        }
      val pose =
        structSchema<Pair<Double, Double>>("Pose2d") { struct("translation", translation) { it } }

      withServer { server, http ->
        val id = server.publishStruct("/Pose", pose)

        http.connect(server) {
          sendText(subscribeMessage(1, "/", prefix = true))

          val first = nextAnnounce()
          first.string("name") shouldBe "/.schema/struct:Translation2d"
          first.string("type") shouldBe "structschema"
          first["properties"]!!.jsonObject.string("retained") shouldBe "true"
          val firstValue = nextValue()
          firstValue[1] shouldBe 0L
          firstValue[2] shouldBe 5L
          String(firstValue[3] as ByteArray, Charsets.UTF_8) shouldBe "float64 x; float64 y"

          nextAnnounce().string("name") shouldBe "/.schema/struct:Pose2d"
          String(nextValue()[3] as ByteArray, Charsets.UTF_8) shouldBe
            "Translation2d translation"

          val topic = nextAnnounce()
          topic.string("name") shouldBe "/Pose"
          topic.string("type") shouldBe "struct:Pose2d"

          server.setStruct(id, pose, 1.0 to 2.0)
          val value = nextValue()
          value[2] shouldBe 5L
          (value[3] as ByteArray).size shouldBe 16
        }
      }
    }

    test("struct arrays use the array type string and pack values back to back") {
      val schema = structSchema<Double>("Value") { float64("v") { it } }

      withServer { server, http ->
        val id = server.publishStructArray("/Values", schema)

        http.connect(server) {
          sendText(subscribeMessage(1, "/Values"))
          nextAnnounce().string("type") shouldBe "struct:Value[]"

          server.setStructArray(id, schema, listOf(1.0, 2.0, 3.0))
          (nextValue()[3] as ByteArray).size shouldBe 24
        }
      }
    }

    test("schemas shared by several topics are published once") {
      val schema = structSchema<Double>("Value") { float64("v") { it } }

      withServer { server, _ ->
        server.publishStruct("/one", schema)
        server.publishStruct("/two", schema)
        server.publishStructArray("/many", schema)

        // The schema topic, then the three topics, are the only four, so the next id is 4.
        server.publish("/after", NT4Type.DOUBLE) shouldBe 4
      }
    }

    test("a different schema under a published type name is rejected") {
      val a = structSchema<Unit>("Thing") { float64("x") { 0.0 } }
      val b = structSchema<Unit>("Thing") { float32("x") { 0f } }
      val server = NT4Server(port = 0)
      server.publishStruct("/a", a)

      shouldThrow<IllegalArgumentException> { server.publishStruct("/b", b) }
    }

    test("clients are answered clock synchronization messages, echoing their value and type") {
      val clock = TestTimeSource()
      withServer(clock) { server, http ->
        http.connect(server) {
          clock += 5.milliseconds
          sendBinary(rttMessage(12_345))

          val reply = nextValue()
          reply[0] shouldBe -1L
          reply[1] shouldBe 5_000L
          reply[2] shouldBe 2L
          reply[3] shouldBe 12_345L
        }
      }
    }

    test("a clock synchronization value that is a float is echoed as a float") {
      withServer { server, http ->
        http.connect(server) {
          val packer = MessagePack.newDefaultBufferPacker()
          packer.packArrayHeader(4)
          packer.packLong(-1)
          packer.packLong(0)
          packer.packLong(1)
          packer.packDouble(1.5)
          sendBinary(packer.toByteArray())

          val reply = nextValue()
          reply[2] shouldBe 1L
          reply[3] shouldBe 1.5
        }
      }
    }

    test("value timestamps are on the same clock as clock synchronization") {
      val clock = TestTimeSource()
      withServer(clock) { server, http ->
        val id = server.publish("/a", NT4Type.DOUBLE)
        clock += 7.milliseconds
        server.setDouble(id, 1.0)

        http.connect(server) {
          sendText(subscribeMessage(1, "/a"))
          nextAnnounce()

          nextValue()[1] shouldBe 7_000L
        }
      }
    }

    test("the version 4.0 protocol works the same way") {
      withServer { server, http ->
        val id = server.publish("/a", NT4Type.DOUBLE)
        server.setDouble(id, 4.0)

        http.connect(server, NT4Server.PROTOCOL_V4_0) {
          sendText(subscribeMessage(1, "/a"))

          nextAnnounce().string("name") shouldBe "/a"
          nextValue()[3] shouldBe 4.0
        }
      }
    }

    test("the RTT protocol answers clock synchronization and nothing else") {
      withServer { server, http ->
        server.publish("/a", NT4Type.DOUBLE)

        http.connect(server, NT4Server.PROTOCOL_RTT) {
          sendText(subscribeMessage(1, "/", prefix = true))
          sendBinary(rttMessage(99))

          val reply = nextValue()
          reply[0] shouldBe -1L
          reply[3] shouldBe 99L
          expectNoFrame()
        }
      }
    }

    test("plain HTTP requests are answered, with CORS allowed for browser dashboards") {
      withServer { server, http ->
        for (path in listOf("/", "/nt/test")) {
          val response = http.get("http://127.0.0.1:${server.boundPort}$path")

          response.status.value shouldBe 200
          response.headers["Access-Control-Allow-Origin"] shouldBe "*"
        }
      }
    }

    test("malformed messages are ignored and the connection keeps working") {
      withServer { server, http ->
        val id = server.publish("/a", NT4Type.DOUBLE)

        http.connect(server) {
          sendText("not json")
          sendText("{}")
          sendText("[1, 2, \"three\"]")
          sendText("""[{"method": 5, "params": {}}, {"method": "subscribe"}]""")
          sendText("""[{"method": "subscribe", "params": {"topics": [1], "subuid": 1}}]""")
          sendText("""[{"method": "subscribe", "params": {"topics": ["/a"], "subuid": "x"}}]""")
          sendBinary(byteArrayOf(0xC1.toByte()))
          sendBinary(byteArrayOf(0x94.toByte(), 0x01))
          sendBinary(MessagePack.newDefaultBufferPacker().apply { packLong(5) }.toByteArray())

          sendText(subscribeMessage(1, "/a"))
          nextAnnounce().string("name") shouldBe "/a"
          server.setDouble(id, 1.0)
          nextValue()[3] shouldBe 1.0
        }
      }
    }

    test("messages the server doesn't handle are ignored") {
      withServer { server, http ->
        server.publish("/a", NT4Type.DOUBLE)

        http.connect(server) {
          sendText(
            """[{"method": "publish", "params": {"name": "/x", "pubuid": 1, "type": "double"}}]""",
          )
          sendText("""[{"method": "setproperties", "params": {"name": "/a", "update": {}}}]""")

          expectNoFrame()
        }
      }
    }

    test("many threads can set values at once without losing or reordering any") {
      withServer { server, http ->
        val ids = List(4) { server.publish("/t$it", NT4Type.INTEGER) }

        http.connect(server) {
          sendText(subscribeMessage(1, "/", prefix = true))
          repeat(4) { nextAnnounce() }

          val threads =
            ids.map { id ->
              Thread { repeat(200) { n -> server.setInteger(id, n.toLong()) } }
            }
          threads.forEach { it.start() }
          threads.forEach { it.join() }

          val received = List(800) { nextValue() }
          for (id in ids) {
            received.filter { it[0] == id.toLong() }.map { it[3] } shouldContainExactly
              List(200) { it.toLong() }
          }
        }
      }
    }

    test("stopping disconnects clients, and the server can be started again") {
      withServer { server, http ->
        val id = server.publish("/a", NT4Type.DOUBLE)
        server.setDouble(id, 1.0)

        http.connect(server) {
          sendText(subscribeMessage(1, "/a"))
          nextAnnounce()
          nextValue()

          server.stop()
          server.isRunning shouldBe false
          withTimeout(5.seconds) { for (frame in incoming) {} }
        }

        server.start()
        server.isRunning shouldBe true
        http.connect(server) {
          sendText(subscribeMessage(1, "/a"))
          nextAnnounce().string("name") shouldBe "/a"
          nextValue()[3] shouldBe 1.0
        }
      }
    }

    test("the server can be restarted on the same port after it has served a client") {
      val probe = java.net.ServerSocket(0)
      val port = probe.localPort
      probe.close()
      val server = NT4Server(port = port, host = "127.0.0.1")
      val http = HttpClient(CIO) { install(WebSockets) }
      try {
        server.start()
        http.connect(server) {
          sendBinary(rttMessage(1))
          nextValue()
        }
        server.stop()

        server.start()

        server.boundPort shouldBe port
      } finally {
        http.close()
        server.stop()
      }
    }

    test("clientCount counts connected dashboards, but not RTT-only connections") {
      withServer { server, http ->
        server.clientCount shouldBe 0

        http.connect(server) {
          // The server has registered this client by the time it answers.
          sendBinary(rttMessage(1))
          nextValue()
          server.clientCount shouldBe 1

          http.connect(server, NT4Server.PROTOCOL_RTT) {
            sendBinary(rttMessage(2))
            nextValue()
            server.clientCount shouldBe 1
          }
        }

        // The server notices the client has gone a moment after the client closes the connection.
        withTimeout(5.seconds) {
          while (server.clientCount != 0) kotlinx.coroutines.delay(20)
        }
      }
    }

    test("a name keeps its type after it is unpublished, and can come back with the same one") {
      val server = NT4Server(port = 0)
      val first = server.publish("/a", NT4Type.DOUBLE)
      server.unpublish(first)

      shouldThrow<IllegalArgumentException> { server.publish("/a", NT4Type.STRING) }
        .message!! shouldContain "earlier"
      val again = server.publish("/a", NT4Type.DOUBLE)

      (again != first) shouldBe true
      server.setDouble(again, 1.0)
    }

    test("a struct topic can't come back as a different struct either") {
      val a = structSchema<Double>("A") { float64("v") { it } }
      val b = structSchema<Double>("B") { float64("v") { it } }
      val server = NT4Server(port = 0)
      server.unpublish(server.publishStruct("/p", a))

      shouldThrow<IllegalArgumentException> { server.publishStruct("/p", b) }
      server.publishStruct("/p", a)
    }

    test("a topic that is unpublished and published again is announced again, with the same type") {
      withServer { server, http ->
        val id = server.publish("/a", NT4Type.DOUBLE)

        http.connect(server) {
          sendText(subscribeMessage(1, "/a"))
          nextAnnounce()

          server.unpublish(id)
          nextMessages().single().jsonObject.string("method") shouldBe "unannounce"

          server.publish("/a", NT4Type.DOUBLE)
          nextAnnounce().string("type") shouldBe "double"
        }
      }
    }

    test("starting twice is rejected, and stopping twice is fine") {
      val server = NT4Server(port = 0, host = "127.0.0.1")
      server.start()
      try {
        shouldThrow<IllegalStateException> { server.start() }
      } finally {
        server.stop()
        server.stop()
      }
    }

    test("publishing returns the existing topic's id, but rejects a different type") {
      val server = NT4Server(port = 0)
      val id = server.publish("/a", NT4Type.DOUBLE)

      server.publish("/a", NT4Type.DOUBLE) shouldBe id
      shouldThrow<IllegalArgumentException> { server.publish("/a", NT4Type.STRING) }
    }

    test("empty and reserved topic names are rejected") {
      val server = NT4Server(port = 0)

      shouldThrow<IllegalArgumentException> { server.publish("", NT4Type.DOUBLE) }
      shouldThrow<IllegalArgumentException> { server.publish("\$clients", NT4Type.DOUBLE) }
    }

    test("a value of the wrong type for its topic is rejected") {
      val server = NT4Server(port = 0)
      val id = server.publish("/a", NT4Type.DOUBLE)

      shouldThrow<IllegalArgumentException> { server.setString(id, "x") }
      shouldThrow<IllegalArgumentException> { server.setBoolean(id, true) }
    }

    test("a value for an unknown or removed topic is rejected") {
      val server = NT4Server(port = 0)
      val id = server.publish("/a", NT4Type.DOUBLE)
      server.unpublish(id)

      shouldThrow<IllegalArgumentException> { server.setDouble(id, 1.0) }
      shouldThrow<IllegalArgumentException> { server.setDouble(99, 1.0) }
    }

    test("a negative timestamp is rejected") {
      val server = NT4Server(port = 0)
      val id = server.publish("/a", NT4Type.DOUBLE)

      shouldThrow<IllegalArgumentException> { server.setDouble(id, 1.0, -1) }
    }

    test("setStruct rejects a topic that isn't a struct topic for that schema") {
      val a = structSchema<Double>("A") { float64("v") { it } }
      val b = structSchema<Double>("B") { float64("v") { it } }
      val server = NT4Server(port = 0)
      val plain = server.publish("/plain", NT4Type.RAW)
      val topicA = server.publishStruct("/a", a)

      shouldThrow<IllegalArgumentException> { server.setStruct(plain, a, 1.0) }
      shouldThrow<IllegalArgumentException> { server.setStruct(topicA, b, 1.0) }
      shouldThrow<IllegalArgumentException> { server.setStructArray(topicA, a, listOf(1.0)) }
    }

    test("a server and a log writer with the same source and origin agree on every timestamp") {
      val clock = TestTimeSource()
      val origin = clock.markNow()
      val server = NT4Server(port = 0, timeSource = clock, origin = origin)
      clock += 2.milliseconds
      val writer = WPILogWriter(ByteArrayOutputStream(), timeSource = clock, origin = origin)
      clock += 3.milliseconds
      val mark = clock.markNow()

      server.now() shouldBe writer.now()
      server.now() shouldBe 5_000
      server.timestampOf(mark) shouldBe writer.timestampOf(mark)
    }

    test("values are stamped relative to an explicit origin") {
      val clock = TestTimeSource()
      val origin = clock.markNow()
      clock += 4.milliseconds
      val server = NT4Server(port = 0, host = "127.0.0.1", timeSource = clock, origin = origin)
      server.start()
      val http = HttpClient(CIO) { install(WebSockets) }
      try {
        val id = server.publish("/a", NT4Type.DOUBLE)
        clock += 1.milliseconds
        server.setDouble(id, 1.0)

        http.connect(server) {
          sendText(subscribeMessage(1, "/a"))
          nextAnnounce()

          nextValue()[1] shouldBe 5_000L
        }
      } finally {
        http.close()
        server.stop()
      }
    }

    test("an origin from another time source or in the future is rejected") {
      val clock = TestTimeSource()

      shouldThrow<IllegalArgumentException> {
        NT4Server(port = 0, timeSource = clock, origin = TestTimeSource().markNow())
      }
      shouldThrow<IllegalArgumentException> {
        NT4Server(port = 0, timeSource = clock, origin = clock.markNow() + 1.seconds)
      }
    }

    test("now and timestampOf are microseconds since the server was created") {
      val clock = TestTimeSource()
      clock += 3.seconds
      val server = NT4Server(port = 0, timeSource = clock)
      val mark = clock.markNow()
      clock += 2.milliseconds

      server.timestampOf(mark) shouldBe 0
      server.now() shouldBe 2_000
      shouldThrow<IllegalArgumentException> { server.timestampOf(TestTimeSource().markNow()) }
    }

    test("each NT4Type has the type string and id the NetworkTables 4 spec gives it") {
      NT4Type.entries.associate { it.typeString to it.id } shouldBe
        mapOf(
          "boolean" to 0,
          "double" to 1,
          "int" to 2,
          "float" to 3,
          "string" to 4,
          "raw" to 5,
          "boolean[]" to 16,
          "double[]" to 17,
          "int[]" to 18,
          "float[]" to 19,
          "string[]" to 20,
          "structschema" to 5,
        )
    }

    test("publishing with an NT4Type and with its type string are the same") {
      val server = NT4Server(port = 0)
      val id = server.publish("/a", NT4Type.DOUBLE)

      server.publish("/a", "double") shouldBe id
    }

    test("the type ids follow the NetworkTables 4 spec") {
      listOf(
        "boolean" to 0,
        "double" to 1,
        "int" to 2,
        "float" to 3,
        "string" to 4,
        "json" to 4,
        "raw" to 5,
        "rpc" to 5,
        "msgpack" to 5,
        "protobuf" to 5,
        "struct:Pose2d" to 5,
        "struct:Pose2d[]" to 5,
        "structschema" to 5,
        "boolean[]" to 16,
        "double[]" to 17,
        "int[]" to 18,
        "float[]" to 19,
        "string[]" to 20,
      )
        .forEach { (type, id) -> NT4Type.idOf(type) shouldBe id }
    }
  })
