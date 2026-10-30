/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.telemetry.sim

import dev.nextftc.telemetry.TelemetryRegistry
import dev.nextftc.telemetry.nt.connect
import dev.nextftc.telemetry.nt.subscribeMessage
import dev.nextftc.telemetry.wpilog.ParsedLog
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldEndWith
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.nio.file.Files
import java.util.Collections
import kotlin.concurrent.thread
import kotlin.time.Duration.Companion.seconds

/** One `announce` or `unannounce` a dashboard saw. */
private data class TopicEvent(
  val method: String,
  val name: String,
  val type: String?,
  val unit: String? = null,
)

/** What a dashboard saw, what telemetry warned, and the logs written, over one short simulation. */
private class SimulationRun(
  val events: List<TopicEvent>,
  val valuesSeen: Int,
  val warnings: List<String>,
  val logs: List<File>,
) {
  val announced: List<TopicEvent> get() = events.filter { it.method == "announce" }

  fun typesOf(name: String): List<String?> = announced.filter { it.name == name }.map { it.type }

  /** The entries a log started, by name, with their types. */
  fun entriesOf(log: File): Map<String, String> = ParsedLog(log.readBytes())
    .records
    .filter { it.isControl }
    .associate { it.startStrings[0] to it.startStrings[1] }
}

/**
 * Plays one short Auto and TeleOp with a dashboard connected, and reports what the dashboard saw.
 *
 * @param conflict Whether TeleOp logs `/Robot/Phase` with a different type than Auto
 */
private suspend fun playOneCycle(conflict: Boolean): SimulationRun {
  val logDir = Files.createTempDirectory("sim-logs").toFile()
  val warnings: MutableList<String> = Collections.synchronizedList(mutableListOf())
  val simulation =
    Simulation(
      SimulatorOptions(
        port = 0,
        autoSeconds = 0.8,
        idleSeconds = 0.1,
        teleopSeconds = 0.8,
        hz = 100,
        extraTopics = 2,
        conflict = conflict,
        cycles = 1,
        logDir = logDir,
      ),
      TelemetryRegistry(),
      warnings::add,
    )
  var failure: Throwable? = null
  val robot =
    thread(name = "simulated-robot") {
      try {
        simulation.run()
      } catch (e: Throwable) {
        failure = e
      }
    }

  val events = mutableListOf<TopicEvent>()
  var valuesSeen = 0
  val http = HttpClient(CIO) { install(WebSockets) }
  try {
    withTimeout(10.seconds) { while (!simulation.server.isRunning) delay(10) }
    http.connect(simulation.server) {
      send(Frame.Text(subscribeMessage(1, "/", prefix = true)))
      // The robot stops its server when it finishes, which ends this loop.
      for (frame in incoming) {
        when (frame) {
          is Frame.Binary -> valuesSeen++
          is Frame.Text ->
            for (message in Json.parseToJsonElement(frame.readText()).jsonArray) {
              val obj = message.jsonObject
              val params = obj["params"]!!.jsonObject
              events +=
                TopicEvent(
                  obj["method"]!!.jsonPrimitive.content,
                  params["name"]!!.jsonPrimitive.content,
                  params["type"]?.jsonPrimitive?.content,
                  params["properties"]?.jsonObject?.get("unit")?.jsonPrimitive?.content,
                )
            }
          else -> {}
        }
      }
    }
  } finally {
    http.close()
    robot.join(10_000)
  }

  failure shouldBe null
  val logs = logDir.listFiles { file -> file.extension == "wpilog" }!!.sortedBy { it.name }
  return SimulationRun(events, valuesSeen, warnings.toList(), logs)
}

class SimulatorTest :
  FunSpec({
    test("a short run streams its topics live, logs both OpModes, and keeps /Debug in the files") {
      val run = playOneCycle(conflict = false)

      run.warnings shouldBe emptyList()

      // The live topics, with the types AdvantageScope needs to draw them.
      run.typesOf("/Robot/Pose").distinct() shouldBe listOf("struct:Pose2d")
      run.typesOf("/Robot/Targets").distinct() shouldBe listOf("struct:Pose2d[]")
      run.typesOf("/.schema/struct:Pose2d").distinct() shouldBe listOf("structschema")
      run.typesOf("/.schema/struct:Translation2d").distinct() shouldBe listOf("structschema")
      run.typesOf("/.schema/struct:Rotation2d").distinct() shouldBe listOf("structschema")
      run.typesOf("/Loop/Count").distinct() shouldBe listOf("int")
      run.typesOf("/Drive/Left/Velocity").distinct() shouldBe listOf("double")
      run.typesOf("/Drive/Enabled").distinct() shouldBe listOf("boolean")
      run.typesOf("/Drive/MotorPowers").distinct() shouldBe listOf("double[]")
      run.typesOf("/Robot/Intake/Jammed").distinct() shouldBe listOf("boolean")
      run.typesOf("/Stress/t0").distinct() shouldBe listOf("double")
      run.typesOf("/Stress/t1").distinct() shouldBe listOf("double")

      // Units are announced every time a topic comes back, and only on the topics that have one.
      run.announced.filter { it.name == "/Battery/Voltage" }.map { it.unit } shouldBe listOf("V", "V")
      run.announced.filter { it.name == "/Drive/Left/Velocity" }.map { it.unit } shouldBe
        listOf("m/s", "m/s")
      run.announced.filter { it.name == "/Loop/Count" }.all { it.unit == null } shouldBe true

      // Only the live paths reach the server.
      run.announced.none { it.name.startsWith("/Debug") } shouldBe true

      // Each OpMode's topics appear when it starts and go when it ends.
      run.typesOf("/Robot/Pose").size shouldBe 2
      run.events.count { it.method == "unannounce" && it.name == "/Robot/Pose" } shouldBe 2

      // The same type both times, so a dashboard never sees a conflict.
      run.typesOf("/Robot/Phase") shouldBe listOf("string", "string")

      (run.valuesSeen > 0) shouldBe true

      // Each OpMode has its own log, and /Debug is in both.
      run.logs.size shouldBe 2
      run.logs[0].name shouldEndWith "__Auto.wpilog"
      run.logs[1].name shouldEndWith "__TeleOp.wpilog"
      for (log in run.logs) {
        val entries = run.entriesOf(log)
        entries["/Debug/LogNanos"] shouldBe "int64"
        entries["/Robot/Pose"] shouldBe "struct:Pose2d"
        entries["/.schema/struct:Pose2d"] shouldBe "structschema"
        entries["/Stress/t1"] shouldBe "double"
        entries["/Robot/Phase"] shouldBe "string"
      }
    }

    test("a path that changes type between OpModes is refused live and warned, but not in the file") {
      val run = playOneCycle(conflict = true)

      // The dashboard only ever sees the first type, so it can't show a conflicting-types error.
      run.typesOf("/Robot/Phase") shouldBe listOf("string")

      // Telemetry reports it once, naming the path.
      run.warnings.size shouldBe 1
      run.warnings[0] shouldContain "/Robot/Phase"
      run.warnings[0] shouldContain "earlier"

      // Each log file is independent, so TeleOp's still has the integer.
      run.entriesOf(run.logs[0])["/Robot/Phase"] shouldBe "string"
      run.entriesOf(run.logs[1])["/Robot/Phase"] shouldBe "int64"
    }

    test("the options default sensibly, and every one can be set") {
      val defaults = SimulatorOptions.parse(emptyArray())
      defaults.port shouldBe 5810
      defaults.hz shouldBe 50
      defaults.cycles shouldBe 0
      defaults.reboot shouldBe false
      defaults.conflict shouldBe false

      val options =
        SimulatorOptions.parse(
          arrayOf(
            "--port", "0",
            "--auto", "1.5",
            "--idle", "0.25",
            "--teleop", "2",
            "--hz", "100",
            "--topics", "7",
            "--reboot", "true",
            "--conflict", "true",
            "--cycles", "3",
            "--log-dir", "somewhere",
          ),
        )
      options.port shouldBe 0
      options.autoSeconds shouldBe 1.5
      options.idleSeconds shouldBe 0.25
      options.teleopSeconds shouldBe 2.0
      options.hz shouldBe 100
      options.extraTopics shouldBe 7
      options.reboot shouldBe true
      options.conflict shouldBe true
      options.cycles shouldBe 3
      options.logDir.path shouldBe "somewhere"
    }

    test("a bad option is rejected with a message that names it") {
      shouldThrow<IllegalArgumentException> { SimulatorOptions.parse(arrayOf("--nonsense", "1")) }
        .message shouldNotBe null
      shouldThrow<IllegalArgumentException> { SimulatorOptions.parse(arrayOf("--port")) }
      shouldThrow<IllegalArgumentException> { SimulatorOptions.parse(arrayOf("port", "1")) }
      shouldThrow<IllegalArgumentException> { SimulatorOptions.parse(arrayOf("--port", "70000")) }
      shouldThrow<IllegalArgumentException> { SimulatorOptions.parse(arrayOf("--hz", "0")) }
      shouldThrow<IllegalArgumentException> { SimulatorOptions.parse(arrayOf("--auto", "-1")) }
      shouldThrow<IllegalArgumentException> { SimulatorOptions.parse(arrayOf("--port", "abc")) }
      shouldThrow<IllegalArgumentException> { SimulatorOptions.parse(arrayOf("--reboot", "maybe")) }
    }
  })
