/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.telemetry

import dev.nextftc.telemetry.backend.MockTelemetryBackend
import dev.nextftc.telemetry.nt.NT4Server
import dev.nextftc.telemetry.wpilog.WPILogWriter
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.io.ByteArrayOutputStream
import java.util.Collections
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource

private class Env {
  val clock = TestTimeSource()
  val registry = TelemetryRegistry(clock)
  val warnings: MutableList<String> = Collections.synchronizedList(mutableListOf())

  init {
    registry.warningHandler = TelemetryWarningHandler { warnings += it }
  }
}

/** A backend whose entries fail when they log a double. */
private class FailingBackend(private val failOnEntry: Boolean = false) : TelemetryBackend {
  override fun entry(path: String, type: TelemetryType, properties: Map<String, Any>): TelemetryEntry {
    if (failOnEntry) throw IllegalStateException("no entry for $path")
    return object : TelemetryEntry by DiscardEntry {
      override fun logDouble(value: Double, timestampMicros: Long): Unit =
        throw IllegalStateException("cannot log")
    }
  }

  override fun close() {}

  override fun toString() = "FailingBackend"
}

private class ClosingBackend(private val fail: Boolean = false) : TelemetryBackend {
  var closed = false

  override fun entry(path: String, type: TelemetryType, properties: Map<String, Any>): TelemetryEntry =
    DiscardEntry

  override fun close() {
    closed = true
    if (fail) throw IllegalStateException("cannot close")
  }
}

class TelemetryRegistryTest :
  FunSpec({
    test("logging with no backend registered is harmless") {
      val env = Env()
      val handle = env.registry.root.doubleTopic("x")

      handle.set(1.0)
      env.registry.root.log("pose", "text")

      env.warnings shouldBe emptyList()
    }

    test("a handle's properties and unit reach the backends when its path is first published") {
      val env = Env()
      val mock = MockTelemetryBackend().also { env.registry.register(it) }
      val speed = env.registry.root.doubleTopic("speed").withUnit("m/s")
      speed.properties = speed.properties + ("limit" to 2)
      env.registry.root.doubleTopic("plain")

      speed.set(1.0)
      env.registry.root.log("plain", 1.0)

      speed.unit shouldBe "m/s"
      mock.propertiesAt("/speed") shouldBe mapOf("unit" to "m/s", "limit" to 2)
      mock.propertiesAt("/plain") shouldBe emptyMap()
      env.warnings shouldBe emptyList()
    }

    test("a property of an unsupported kind is reported and left out, and the unit can be cleared") {
      val env = Env()
      val handle = env.registry.root.doubleTopic("x")

      handle.properties = mapOf("unit" to "m", "bad" to listOf(1), "nan" to Double.NaN)

      handle.properties shouldBe mapOf("unit" to "m")
      env.warnings.size shouldBe 2
      env.warnings.all { it.contains("/x") } shouldBe true
      handle.unit = null
      handle.unit shouldBe null
      handle.properties shouldBe emptyMap()
    }

    test("a handle made before any backend starts working once one is registered") {
      val env = Env()
      val handle = env.registry.table("T").doubleTopic("x")
      handle.set(1.0)
      val mock = MockTelemetryBackend().also { env.registry.register(it) }
      handle.set(2.0)

      mock.valuesAt("/T/x") shouldBe listOf(2.0)
    }

    test("a handle keeps working when one backend replaces another, as between OpModes") {
      val env = Env()
      val handle = env.registry.table("T").doubleTopic("x")
      val first = MockTelemetryBackend().also { env.registry.register(it) }
      handle.set(1.0)
      env.registry.unregister(first)
      val second = MockTelemetryBackend().also { env.registry.register(it) }
      handle.set(2.0)

      first.valuesAt("/T/x") shouldBe listOf(1.0)
      second.valuesAt("/T/x") shouldBe listOf(2.0)
      second.typeAt("/T/x") shouldBe ValueType.DOUBLE
    }

    test("a path can change type once the backend that had it is replaced, as between OpModes") {
      val env = Env()
      val first = MockTelemetryBackend().also { env.registry.register(it) }
      env.registry.root.log("x", 1.0)
      env.registry.unregister(first)
      val second = MockTelemetryBackend().also { env.registry.register(it) }
      env.registry.root.log("x", "text")

      second.typeAt("/x") shouldBe ValueType.STRING
      second.valuesAt("/x") shouldBe listOf("text")
      env.warnings shouldBe emptyList()
    }

    test("a path can't change type while the same backend stays registered") {
      val env = Env()
      val mock = MockTelemetryBackend().also { env.registry.register(it) }
      env.registry.root.log("x", 1.0)
      env.registry.root.log("x", "text")

      mock.typeAt("/x") shouldBe ValueType.DOUBLE
      mock.valuesAt("/x") shouldBe listOf(1.0)
      env.warnings.size shouldBe 1
      env.warnings[0] shouldContain "/x"
    }

    test("each backend keeps its own types, so a later one isn't bound by an earlier one") {
      val env = Env()
      val first = MockTelemetryBackend().also { env.registry.register(it) }
      env.registry.root.log("x", 1.0)
      val second = MockTelemetryBackend().also { env.registry.register(it) }
      env.registry.root.log("x", "text")

      first.valuesAt("/x") shouldBe listOf(1.0)
      second.valuesAt("/x") shouldBe listOf("text")
      env.warnings.size shouldBe 1
    }

    test("registering a backend again starts its types over") {
      val env = Env()
      val mock = MockTelemetryBackend()
      env.registry.register(mock)
      env.registry.root.log("x", 1.0)
      env.registry.unregister(mock)
      env.registry.register(mock)
      env.registry.root.log("x", "text")

      mock.typeAt("/x") shouldBe ValueType.STRING
      env.warnings shouldBe emptyList()
    }

    test("a new backend gets the next value even if it repeats the last") {
      val env = Env()
      val handle = env.registry.root.doubleTopic("x")
      val first = MockTelemetryBackend().also { env.registry.register(it) }
      handle.set(1.0)
      val second = MockTelemetryBackend().also { env.registry.register(it) }
      handle.set(1.0)

      second.valuesAt("/x") shouldBe listOf(1.0)
      first.valuesAt("/x").size shouldBe 2
    }

    test("every backend gets every value with the same timestamp") {
      val env = Env()
      val a = MockTelemetryBackend().also { env.registry.register(it) }
      val b = MockTelemetryBackend().also { env.registry.register(it) }
      env.clock += 7.milliseconds
      env.registry.root.log("x", 1.0)

      a.records shouldBe b.records
      a.timestampsAt("/x") shouldBe listOf(7_000L)
    }

    test("a backend registered with prefixes only gets paths under them") {
      val env = Env()
      val drive = MockTelemetryBackend().also { env.registry.register(it, "/Drive") }
      val all = MockTelemetryBackend().also { env.registry.register(it) }
      env.registry.table("Drive").log("x", 1.0)
      env.registry.table("Drive/Sub").log("y", 2.0)
      env.registry.table("Driver").log("z", 3.0)
      env.registry.root.log("Drive", 4.0)
      env.registry.root.log("Other", 5.0)

      drive.paths shouldBe listOf("/Drive/x", "/Drive/Sub/y", "/Drive")
      all.paths.size shouldBe 5
    }

    test("prefixes are normalized, and several can be given") {
      val env = Env()
      val mock = MockTelemetryBackend().also { env.registry.register(it, "Drive/", "/Vision") }
      env.registry.table("Drive").log("x", 1.0)
      env.registry.table("Vision").log("y", 2.0)
      env.registry.table("Other").log("z", 3.0)

      mock.paths shouldBe listOf("/Drive/x", "/Vision/y")
    }

    test("a prefix of slash or nothing matches everything") {
      val env = Env()
      val mock = MockTelemetryBackend().also { env.registry.register(it, "/", "") }
      env.registry.table("Anything").log("x", 1.0)

      mock.paths shouldBe listOf("/Anything/x")
    }

    test("a backend that throws when logging doesn't stop the others, and is reported once") {
      val env = Env()
      env.registry.register(FailingBackend())
      val mock = MockTelemetryBackend().also { env.registry.register(it) }
      val handle = env.registry.root.doubleTopic("x")
      handle.set(1.0)
      handle.set(2.0)

      mock.valuesAt("/x") shouldBe listOf(1.0, 2.0)
      env.warnings.size shouldBe 1
      env.warnings[0] shouldContain "cannot log"
      env.warnings[0] shouldContain "/x"
    }

    test("a backend that can't make an entry is left out of that path, and reported") {
      val env = Env()
      env.registry.register(FailingBackend(failOnEntry = true))
      val mock = MockTelemetryBackend().also { env.registry.register(it) }
      env.registry.root.log("x", 1.0)

      mock.valuesAt("/x") shouldBe listOf(1.0)
      env.warnings.size shouldBe 1
      env.warnings[0] shouldContain "FailingBackend"
      env.warnings[0] shouldContain "/x"
    }

    test("unregistering stops values without closing the backend") {
      val env = Env()
      val backend = ClosingBackend()
      val mock = MockTelemetryBackend().also { env.registry.register(it) }
      env.registry.register(backend)
      env.registry.unregister(mock) shouldBe true
      env.registry.unregister(mock) shouldBe false
      env.registry.root.log("x", 1.0)

      mock.valuesAt("/x") shouldBe emptyList()
      mock.isClosed shouldBe false
      backend.closed shouldBe false
      env.registry.backendCount shouldBe 1
    }

    test("reset closes every backend once and removes them") {
      val env = Env()
      val a = ClosingBackend()
      val b = ClosingBackend()
      env.registry.register(a)
      env.registry.register(b, "/one")
      env.registry.register(b, "/two")
      env.registry.reset()

      a.closed shouldBe true
      b.closed shouldBe true
      env.registry.backendCount shouldBe 0
    }

    test("a backend that fails to close doesn't stop the rest closing, and is reported") {
      val env = Env()
      val bad = ClosingBackend(fail = true)
      val good = ClosingBackend()
      env.registry.register(bad)
      env.registry.register(good)
      env.registry.reset()

      good.closed shouldBe true
      env.warnings.size shouldBe 1
      env.warnings[0] shouldContain "cannot close"
    }

    test("a warning is reported once, until a reset") {
      val env = Env()
      env.registry.warn("same")
      env.registry.warn("same")
      env.registry.warn("different")
      env.registry.reset()
      env.registry.warn("same")

      env.warnings shouldBe listOf("same", "different", "same")
    }

    test("only the first thousand different warnings are reported") {
      val env = Env()
      repeat(1_005) { env.registry.warn("warning $it") }

      env.warnings.size shouldBe 1_000
    }

    test("a warning handler that throws is contained") {
      val env = Env()
      env.registry.warningHandler = TelemetryWarningHandler { throw IllegalStateException("handler") }
      env.registry.root.log("x", 1.0)
      env.registry.root.log("x", "text")
    }

    test("the registry's clock counts microseconds from its origin") {
      val env = Env()
      env.clock += 2.seconds

      env.registry.now() shouldBe 2_000_000
      env.registry.timestampOf(env.clock.markNow()) shouldBe 2_000_000
    }

    test("an origin is timestamp zero, and a mark before it is rejected") {
      val clock = TestTimeSource()
      val early = clock.markNow()
      clock += 1.seconds
      val origin = clock.markNow()
      clock += 5.milliseconds
      val registry = TelemetryRegistry(clock, origin)

      registry.now() shouldBe 5_000
      shouldThrow<IllegalArgumentException> { registry.timestampOf(early) }
    }

    test("an origin from another time source or in the future is rejected") {
      val clock = TestTimeSource()

      shouldThrow<IllegalArgumentException> { TelemetryRegistry(clock, TestTimeSource().markNow()) }
      shouldThrow<IllegalArgumentException> { TelemetryRegistry(clock, clock.markNow() + 1.seconds) }
    }

    test("a registry, a log writer and a server given one source and origin agree on time") {
      val clock = TestTimeSource()
      val origin = clock.markNow()
      clock += 3.milliseconds
      val registry = TelemetryRegistry(clock, origin)
      val writer = WPILogWriter(ByteArrayOutputStream(), timeSource = clock, origin = origin)
      val server = NT4Server(port = 0, timeSource = clock, origin = origin)
      clock += 4.milliseconds

      registry.now() shouldBe 7_000
      writer.now() shouldBe 7_000
      server.now() shouldBe 7_000
    }

    test("many threads can log through their own handles at once") {
      val env = Env()
      val mock = MockTelemetryBackend().also { env.registry.register(it) }
      val handles = List(8) { env.registry.table("T").intTopic("t$it") }

      val threads =
        handles.map { handle -> Thread { repeat(500) { n -> handle.set(n) } } }
      threads.forEach { it.start() }
      threads.forEach { it.join() }

      for (i in 0 until 8) {
        mock.valuesAt("/T/t$i") shouldBe List(500) { it.toLong() }
      }
      env.warnings shouldBe emptyList()
    }

    test("backends can change while threads are logging without losing the handle") {
      val env = Env()
      val handle = env.registry.root.intTopic("x")
      val seen = MockTelemetryBackend().also { env.registry.register(it) }
      val logger = Thread { repeat(2_000) { n -> handle.set(n) } }
      logger.start()
      repeat(50) {
        val extra = MockTelemetryBackend()
        env.registry.register(extra)
        env.registry.unregister(extra)
      }
      logger.join()

      seen.valuesAt("/x") shouldBe List(2_000) { it.toLong() }
      env.warnings shouldBe emptyList()
    }

    test("Telemetry logs through its shared registry, for Kotlin and Java style calls") {
      val mock = MockTelemetryBackend()
      Telemetry.registry.register(mock)
      try {
        Telemetry.log("facade/double", 1.5)
        Telemetry.log("facade/int", 2)
        Telemetry.log("facade/string", "s")
        Telemetry.log("facade/any", 3L as Any)
        Telemetry.table("facade").log("table", true)
        Telemetry.doubleTopic("facade/handle").set(4.0)
        Telemetry.intArrayTopic("facade/ints").set(intArrayOf(5))
        Telemetry.logArray("facade/list", listOf("not a struct".length))
        Telemetry.root.log("facade/root", 6.0)

        mock.valuesAt("/facade/double") shouldBe listOf(1.5)
        mock.valuesAt("/facade/int") shouldBe listOf(2L)
        mock.valuesAt("/facade/string") shouldBe listOf("s")
        mock.valuesAt("/facade/any") shouldBe listOf(3L)
        mock.valuesAt("/facade/table") shouldBe listOf(true)
        mock.valuesAt("/facade/handle") shouldBe listOf(4.0)
        mock.valuesAt("/facade/root") shouldBe listOf(6.0)
      } finally {
        Telemetry.registry.unregister(mock)
      }
    }
  })
