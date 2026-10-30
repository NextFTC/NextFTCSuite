/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.telemetry.backend

import dev.nextftc.telemetry.TelemetryRegistry
import dev.nextftc.telemetry.ValueType
import dev.nextftc.telemetry.struct.structSchema
import dev.nextftc.telemetry.withUnit
import dev.nextftc.telemetry.wpilog.ParsedLog
import dev.nextftc.telemetry.wpilog.WPILogBackend
import dev.nextftc.telemetry.wpilog.WPILogWriter
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import java.io.ByteArrayOutputStream
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TestTimeSource

private val vec =
  structSchema<Pair<Double, Double>>("Translation2d") {
    float64("x") { it.first }
    float64("y") { it.second }
  }

/** Logs through a registry into a WPILOG, closes it, and parses what was written. */
private fun logged(block: (registry: TelemetryRegistry, clock: TestTimeSource) -> Unit): ParsedLog {
  val clock = TestTimeSource()
  val registry = TelemetryRegistry(clock)
  val bytes = ByteArrayOutputStream()
  registry.register(WPILogBackend(WPILogWriter(bytes, timeSource = clock, origin = registry.origin)))
  block(registry, clock)
  registry.reset()
  return ParsedLog(bytes.toByteArray())
}

class WPILogBackendTest :
  FunSpec({
    test("an entry is started on a path's first value, and values carry the registry's timestamps") {
      val log =
        logged { registry, clock ->
          val handle = registry.root.doubleTopic("x")
          clock += 3.milliseconds
          handle.set(1.0)
          clock += 4.milliseconds
          handle.set(2.0)
        }

      val start = log.records.single { it.isControl }
      start.startStrings shouldContainExactly listOf("/x", "double", "")
      val data = log.records.filterNot { it.isControl }
      data.map { it.timestamp } shouldContainExactly listOf(3_000L, 7_000L)
      data.map { it.double } shouldContainExactly listOf(1.0, 2.0)
    }

    test("properties are written as entry metadata the way WPILib logs NetworkTables topics") {
      val log =
        logged { registry, _ ->
          registry.root.doubleTopic("speed").withUnit("m/s").set(1.0)
          registry.root.topic("pose", vec).withUnit("m").set(1.0 to 2.0)
          registry.root.log("plain", 1.0)
        }

      val starts = log.records.filter {
        it.isControl
      }.associate { it.startStrings[0] to it.startStrings[2] }
      starts["/speed"] shouldBe """{"properties":{"unit":"m/s"},"source":"NT"}"""
      starts["/pose"] shouldBe """{"properties":{"unit":"m"},"source":"NT"}"""
      starts["/plain"] shouldBe ""
    }

    test("every type is started with its WPILOG type string") {
      val log =
        logged { registry, _ ->
          val table = registry.root
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
        }

      log.records
        .filter { it.isControl }
        .map { it.startStrings.take(2) } shouldContainExactly
        listOf(
          listOf("/boolean", "boolean"),
          listOf("/int", "int64"),
          listOf("/float", "float"),
          listOf("/double", "double"),
          listOf("/string", "string"),
          listOf("/booleans", "boolean[]"),
          listOf("/ints", "int64[]"),
          listOf("/floats", "float[]"),
          listOf("/doubles", "double[]"),
          listOf("/strings", "string[]"),
        )
    }

    test("values are written in the format of their type") {
      val log =
        logged { registry, _ ->
          registry.root.log("int", -5L)
          registry.root.log("string", "héllo")
          registry.root.log("doubles", doubleArrayOf(1.0, 2.0))
        }

      val data = log.records.filterNot { it.isControl }
      data[0].long shouldBe -5L
      data[1].text shouldBe "héllo"
      data[2].payload.size shouldBe 16
    }

    test("a struct writes its schema entries first, and a struct array its own type") {
      val log =
        logged { registry, _ ->
          registry.root.topic("pose", vec).set(1.0 to 2.0)
          registry.root.arrayTopic("path", vec).set(listOf(1.0 to 2.0, 3.0 to 4.0))
        }

      log.records
        .filter { it.isControl }
        .map { it.startStrings.take(2) } shouldContainExactly
        listOf(
          listOf("/.schema/struct:Translation2d", "structschema"),
          listOf("/pose", "struct:Translation2d"),
          listOf("/path", "struct:Translation2d[]"),
        )
      val data = log.records.filterNot { it.isControl }
      data[0].text shouldBe "float64 x; float64 y"
      data[1].payload.size shouldBe 16
      data[2].payload.size shouldBe 32
    }

    test("asking again for a path returns the same entry and starts it once") {
      val clock = TestTimeSource()
      val bytes = ByteArrayOutputStream()
      val backend = WPILogBackend(WPILogWriter(bytes, timeSource = clock))

      val first = backend.entry("/a", ValueType.DOUBLE)
      val second = backend.entry("/a", ValueType.DOUBLE)
      backend.close()

      (first === second) shouldBe true
      ParsedLog(bytes.toByteArray()).records.count { it.isControl } shouldBe 1
    }

    test("a path can't change type") {
      val backend = WPILogBackend(WPILogWriter(ByteArrayOutputStream()))
      backend.entry("/a", ValueType.DOUBLE)

      shouldThrow<IllegalArgumentException> { backend.entry("/a", ValueType.STRING) }
      backend.close()
    }

    test("closing writes the log out, and entries then drop values quietly") {
      val bytes = ByteArrayOutputStream()
      val backend = WPILogBackend(WPILogWriter(bytes))
      val entry = backend.entry("/a", ValueType.DOUBLE)
      entry.logDouble(1.0, 5)
      backend.close()
      entry.logDouble(2.0, 6)
      backend.close()

      val data = ParsedLog(bytes.toByteArray()).records.filterNot { it.isControl }
      data.map { it.double } shouldContainExactly listOf(1.0)
    }

    test("a repeated value is written once, because the registry skips duplicates") {
      val log =
        logged { registry, _ ->
          val handle = registry.root.intTopic("n")
          repeat(3) { handle.set(7) }
          handle.set(8)
        }

      log.records.filterNot { it.isControl }.map { it.long } shouldContainExactly listOf(7L, 8L)
    }
  })
