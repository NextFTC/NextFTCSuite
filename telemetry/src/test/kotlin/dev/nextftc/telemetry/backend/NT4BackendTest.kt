/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.telemetry.backend

import dev.nextftc.telemetry.TelemetryRegistry
import dev.nextftc.telemetry.TelemetryWarningHandler
import dev.nextftc.telemetry.nt.NT4Backend
import dev.nextftc.telemetry.nt.NT4Server
import dev.nextftc.telemetry.nt.NT4Type
import dev.nextftc.telemetry.nt.connect
import dev.nextftc.telemetry.nt.expectNoFrame
import dev.nextftc.telemetry.nt.nextAnnounce
import dev.nextftc.telemetry.nt.nextMessages
import dev.nextftc.telemetry.nt.nextValue
import dev.nextftc.telemetry.nt.sendText
import dev.nextftc.telemetry.nt.string
import dev.nextftc.telemetry.nt.subscribeMessage
import dev.nextftc.telemetry.struct.structSchema
import dev.nextftc.telemetry.withProperties
import dev.nextftc.telemetry.withUnit
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.WebSockets
import kotlinx.serialization.json.jsonObject
import java.util.Collections
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TestTimeSource

private val vec =
  structSchema<Pair<Double, Double>>("Translation2d") {
    float64("x") { it.first }
    float64("y") { it.second }
  }

private class Rig(val clock: TestTimeSource, val registry: TelemetryRegistry, val server: NT4Server) {
  val warnings: MutableList<String> = Collections.synchronizedList(mutableListOf())
  val backend = NT4Backend(server)

  init {
    registry.warningHandler = TelemetryWarningHandler { warnings += it }
    registry.register(backend)
  }
}

private suspend fun withRig(block: suspend (rig: Rig, http: HttpClient) -> Unit) {
  val clock = TestTimeSource()
  val registry = TelemetryRegistry(clock)
  val server = NT4Server(port = 0, host = "127.0.0.1", timeSource = clock, origin = registry.origin)
  server.start()
  val http = HttpClient(CIO) { install(WebSockets) }
  try {
    block(Rig(clock, registry, server), http)
  } finally {
    http.close()
    registry.reset()
    server.stop()
  }
}

class NT4BackendTest :
  FunSpec({
    test("a topic is published on a path's first value, with the registry's timestamp") {
      withRig { rig, http ->
        val handle = rig.registry.table("Robot").doubleTopic("battery")

        http.connect(rig.server) {
          sendText(subscribeMessage(1, "/", prefix = true))
          expectNoFrame()

          rig.clock += 5.milliseconds
          handle.set(12.5)

          val announce = nextAnnounce()
          announce.string("name") shouldBe "/Robot/battery"
          announce.string("type") shouldBe "double"
          val value = nextValue()
          value[1] shouldBe 5_000L
          value[3] shouldBe 12.5
        }
      }
    }

    test("a handle's unit and properties are announced with its topic") {
      withRig { rig, http ->
        rig.registry.root.doubleTopic("speed").withUnit("m/s").set(1.0)
        val poseHandle = rig.registry.root.topic("pose", vec)
        poseHandle.withProperties("unit" to "m", "note" to "field").set(1.0 to 2.0)

        http.connect(rig.server) {
          sendText(subscribeMessage(1, "/", prefix = true))

          val speed = nextAnnounce()
          speed.string("name") shouldBe "/speed"
          speed["properties"]!!.jsonObject.string("unit") shouldBe "m/s"
          nextValue()
          nextAnnounce().string("name") shouldBe "/.schema/struct:Translation2d"
          nextValue()
          val pose = nextAnnounce()
          pose.string("name") shouldBe "/pose"
          pose["properties"]!!.jsonObject.string("unit") shouldBe "m"
          pose["properties"]!!.jsonObject.string("note") shouldBe "field"
        }
      }
    }

    test("every type is published with its NetworkTables type") {
      withRig { rig, http ->
        val table = rig.registry.root
        table.log("boolean", true)
        table.log("int", 1)
        table.log("float", 1f)
        table.log("double", 1.0)
        table.log("string", "s")
        table.log("booleans", booleanArrayOf(true))
        table.log("ints", intArrayOf(1))
        table.log("floats", floatArrayOf(1f))
        table.log("doubles", doubleArrayOf(1.0))
        table.stringArrayTopic("strings").set(listOf("s"))

        http.connect(rig.server) {
          sendText(subscribeMessage(1, "/", prefix = true))
          val announced = List(10) {
            val announce = nextAnnounce()
            nextValue()
            announce.string("name") to announce.string("type")
          }

          announced shouldContainExactly
            listOf(
              "/boolean" to "boolean",
              "/int" to "int",
              "/float" to "float",
              "/double" to "double",
              "/string" to "string",
              "/booleans" to "boolean[]",
              "/ints" to "int[]",
              "/floats" to "float[]",
              "/doubles" to "double[]",
              "/strings" to "string[]",
            )
        }
      }
    }

    test("a struct publishes its schema topic, then the struct topic with packed values") {
      withRig { rig, http ->
        rig.registry.root.topic("pose", vec).set(1.0 to 2.0)

        http.connect(rig.server) {
          sendText(subscribeMessage(1, "/", prefix = true))

          nextAnnounce().string("name") shouldBe "/.schema/struct:Translation2d"
          String(nextValue()[3] as ByteArray, Charsets.UTF_8) shouldBe "float64 x; float64 y"
          val topic = nextAnnounce()
          topic.string("name") shouldBe "/pose"
          topic.string("type") shouldBe "struct:Translation2d"
          (nextValue()[3] as ByteArray).size shouldBe 16
        }
      }
    }

    test("closing removes the topics it published, and leaves the server running") {
      withRig { rig, http ->
        rig.registry.root.log("x", 1.0)

        http.connect(rig.server) {
          sendText(subscribeMessage(1, "/", prefix = true))
          nextAnnounce()
          nextValue()

          rig.backend.close()

          val message = nextMessages().single().jsonObject
          message.string("method") shouldBe "unannounce"
          message["params"]!!.jsonObject.string("name") shouldBe "/x"
        }
        rig.server.isRunning shouldBe true
      }
    }

    test("closing can stop the server too") {
      val server = NT4Server(port = 0, host = "127.0.0.1")
      server.start()

      NT4Backend(server, stopServerOnClose = true).close()

      server.isRunning shouldBe false
    }

    test("the next backend can publish the same names again, as the next OpMode does") {
      withRig { rig, http ->
        rig.registry.root.log("x", 1.0)
        rig.registry.unregister(rig.backend)
        rig.backend.close()
        val next = NT4Backend(rig.server)
        rig.registry.register(next)
        rig.registry.root.log("x", 2.0)

        http.connect(rig.server) {
          sendText(subscribeMessage(1, "/x"))

          nextAnnounce().string("type") shouldBe "double"
          nextValue()[3] shouldBe 2.0
        }
        rig.warnings shouldBe emptyList()
      }
    }

    test("the next backend can't give a name another type, since dashboards keep the first one") {
      withRig { rig, http ->
        rig.registry.root.log("x", 1.0)
        rig.registry.unregister(rig.backend)
        rig.backend.close()
        val next = NT4Backend(rig.server)
        rig.registry.register(next)
        rig.registry.root.log("x", "text")

        rig.warnings.size shouldBe 1
        rig.warnings[0] shouldContain "/x"
        rig.warnings[0] shouldContain "earlier"
        http.connect(rig.server) {
          sendText(subscribeMessage(1, "/x"))

          // The conflicting topic was never published, so the dashboard never sees the new type.
          expectNoFrame()
        }
      }
    }

    test("a name the server already publishes with another type is reported, not thrown") {
      withRig { rig, _ ->
        rig.server.publish("/x", NT4Type.STRING)
        rig.registry.root.log("x", 1.0)

        rig.warnings.size shouldBe 1
        rig.warnings[0] shouldContain "/x"
      }
    }

    test("entries drop values quietly after the backend is closed") {
      withRig { rig, http ->
        val handle = rig.registry.root.doubleTopic("x")
        handle.set(1.0)
        rig.backend.close()

        handle.set(2.0)

        rig.warnings shouldBe emptyList()
        http.connect(rig.server) {
          sendText(subscribeMessage(1, "/", prefix = true))
          expectNoFrame()
        }
      }
    }

    test("a path that changes type is refused by the backend") {
      withRig { rig, _ ->
        rig.backend.entry("/x", dev.nextftc.telemetry.ValueType.DOUBLE)

        io.kotest.assertions.throwables.shouldThrow<IllegalArgumentException> {
          rig.backend.entry("/x", dev.nextftc.telemetry.ValueType.STRING)
        }
      }
    }
  })
