/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.telemetry.wpilog

import dev.nextftc.telemetry.struct.structSchema
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.time.Duration.Companion.microseconds
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource

/**
 * A minimal WPILOG reader written from the format spec, independent of the writer, so these tests
 * check the bytes rather than the writer against itself.
 */
internal class ParsedLog(bytes: ByteArray) {
  class Record(val entryId: Int, val timestamp: Long, val payload: ByteArray) {
    private val buffer get() = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN)

    val isControl get() = entryId == 0
    val controlType get() = payload[0].toInt()
    val text get() = String(payload, Charsets.UTF_8)
    val long get() = buffer.getLong()
    val double get() = buffer.getDouble()
    val controlEntryId get() = ByteBuffer.wrap(payload, 1, 4).order(ByteOrder.LITTLE_ENDIAN).int

    /** The strings of a start record, in order: name, type, metadata. */
    val startStrings: List<String>
      get() {
        val b = buffer.position(5) as ByteBuffer
        return List(3) {
          val length = b.getInt()
          ByteArray(length).also { b.get(it) }.toString(Charsets.UTF_8)
        }
      }
  }

  val version: Int
  val extraHeader: String
  val records = mutableListOf<Record>()

  init {
    val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
    val magic = ByteArray(6).also { buffer.get(it) }.toString(Charsets.US_ASCII)
    check(magic == "WPILOG") { "bad magic '$magic'" }
    version = buffer.short.toInt() and 0xFFFF
    extraHeader = ByteArray(buffer.int).also { buffer.get(it) }.toString(Charsets.UTF_8)
    while (buffer.hasRemaining()) {
      val bitfield = buffer.get().toInt()
      val idLength = (bitfield and 0x3) + 1
      val sizeLength = ((bitfield shr 2) and 0x3) + 1
      val timestampLength = ((bitfield shr 4) and 0x7) + 1
      val id = unsigned(buffer, idLength).toInt()
      val size = unsigned(buffer, sizeLength).toInt()
      val timestamp = unsigned(buffer, timestampLength)
      records += Record(id, timestamp, ByteArray(size).also { buffer.get(it) })
    }
  }

  private fun unsigned(buffer: ByteBuffer, length: Int): Long {
    var value = 0L
    for (i in 0 until length) value = value or ((buffer.get().toLong() and 0xFF) shl (8 * i))
    return value
  }
}

private fun log(extraHeader: String = "", block: (WPILogWriter) -> Unit): ParsedLog {
  val bytes = ByteArrayOutputStream()
  WPILogWriter(bytes, extraHeader).use(block)
  return ParsedLog(bytes.toByteArray())
}

class WPILogWriterTest :
  FunSpec({
    test("header declares version 1.0 and carries the extra header") {
      val parsed = log("""{"robot":"test"}""") {}

      parsed.version shouldBe 0x0100
      parsed.extraHeader shouldBe """{"robot":"test"}"""
      parsed.records.size shouldBe 0
    }

    test("start records carry id, name, type, and metadata, and ids count up from 1") {
      val parsed =
        log {
          it.startEntry("/a", WPILogType.DOUBLE, 10) shouldBe 1
          it.startEntry("/b", WPILogType.STRING, 20, metadata = "{\"k\":1}") shouldBe 2
        }

      val (first, second) = parsed.records
      first.isControl shouldBe true
      first.controlType shouldBe 0
      first.controlEntryId shouldBe 1
      first.timestamp shouldBe 10
      first.startStrings shouldContainExactly listOf("/a", "double", "")
      second.controlEntryId shouldBe 2
      second.startStrings shouldContainExactly listOf("/b", "string", "{\"k\":1}")
    }

    test("finish and set-metadata records") {
      val parsed =
        log {
          val id = it.startEntry("/a", WPILogType.DOUBLE, 0)
          it.setMetadata(id, "meta", 5)
          it.finishEntry(id, 6)
        }

      val (_, setMetadata, finish) = parsed.records
      setMetadata.controlType shouldBe 2
      setMetadata.controlEntryId shouldBe 1
      setMetadata.payload.copyOfRange(5, 9).toList() shouldBe listOf<Byte>(4, 0, 0, 0)
      String(setMetadata.payload, 9, 4, Charsets.UTF_8) shouldBe "meta"
      finish.controlType shouldBe 1
      finish.controlEntryId shouldBe 1
      finish.timestamp shouldBe 6
    }

    test("scalar values round-trip") {
      val parsed =
        log {
          val b = it.startEntry("/b", WPILogType.BOOLEAN, 0)
          val i = it.startEntry("/i", WPILogType.INTEGER, 0)
          val f = it.startEntry("/f", WPILogType.FLOAT, 0)
          val d = it.startEntry("/d", WPILogType.DOUBLE, 0)
          val s = it.startEntry("/s", WPILogType.STRING, 0)
          it.appendBoolean(b, true, 1)
          it.appendInteger(i, -2L, 2)
          it.appendFloat(f, 1.5f, 3)
          it.appendDouble(d, Math.PI, 4)
          it.appendString(s, "héllo", 5)
        }

      val data = parsed.records.filterNot { it.isControl }
      data[0].payload.toList() shouldBe listOf<Byte>(1)
      data[1].long shouldBe -2L
      data[2].payload.size shouldBe 4
      ByteBuffer.wrap(data[2].payload).order(ByteOrder.LITTLE_ENDIAN).float shouldBe 1.5f
      data[3].double shouldBe Math.PI
      data[4].text shouldBe "héllo"
      data.map { it.timestamp } shouldContainExactly listOf(1L, 2L, 3L, 4L, 5L)
    }

    test("array values round-trip") {
      val parsed =
        log {
          val d = it.startEntry("/d", WPILogType.DOUBLE_ARRAY, 0)
          val s = it.startEntry("/s", WPILogType.STRING_ARRAY, 0)
          val b = it.startEntry("/b", WPILogType.BOOLEAN_ARRAY, 0)
          it.appendDoubleArray(d, doubleArrayOf(1.0, 2.0), 1)
          it.appendStringArray(s, listOf("a", "bc"), 1)
          it.appendBooleanArray(b, booleanArrayOf(true, false, true), 1)
        }

      val (doubles, strings, booleans) = parsed.records.filterNot { it.isControl }
      doubles.payload.size shouldBe 16
      ByteBuffer.wrap(doubles.payload).order(ByteOrder.LITTLE_ENDIAN).getDouble(8) shouldBe 2.0
      // count=2, len=1 "a", len=2 "bc"
      strings.payload.size shouldBe 4 + (4 + 1) + (4 + 2)
      booleans.payload.toList() shouldBe listOf<Byte>(1, 0, 1)
    }

    test("ids, sizes, and timestamps use the fewest bytes and large values survive") {
      val bigPayload = ByteArray(70_000) { it.toByte() }
      val parsed =
        log {
          val id = it.startEntry("/raw", WPILogType.RAW, 0)
          it.appendRaw(id, byteArrayOf(1), 0)
          it.appendRaw(id, byteArrayOf(2), 255)
          it.appendRaw(id, byteArrayOf(3), 256)
          it.appendRaw(id, byteArrayOf(4), 0x1_0000_0000L)
          it.appendRaw(id, bigPayload, 7)
          it.appendRaw(id, byteArrayOf(5), -1)
        }

      val data = parsed.records.filterNot { it.isControl }
      data.map { it.timestamp } shouldContainExactly
        listOf(0L, 255L, 256L, 0x1_0000_0000L, 7L, -1L)
      data[4].payload.contentEquals(bigPayload) shouldBe true
    }

    test("an entry id above 255 is written in two bytes") {
      val parsed =
        log { writer ->
          var last = 0
          repeat(300) { last = writer.startEntry("/e$it", WPILogType.RAW, 0) }
          writer.appendRaw(last, byteArrayOf(9), 0)
        }

      val data = parsed.records.last()
      data.entryId shouldBe 300
      data.payload.toList() shouldBe listOf<Byte>(9)
    }

    test("struct entries write nested schemas first, then the data entry") {
      val translation =
        structSchema<Pair<Double, Double>>("Translation2d") {
          float64("x") { it.first }
          float64("y") { it.second }
        }
      val pose =
        structSchema<Pair<Double, Double>>("Pose2d") {
          struct("translation", translation) { it }
        }

      val parsed =
        log {
          val id = it.startStructEntry("/Pose", pose, 0)
          it.appendStruct(id, pose, 1.0 to 2.0, 5)
        }

      val control = parsed.records.filter { it.isControl }
      control.map { it.startStrings.take(2) } shouldContainExactly
        listOf(
          listOf("/.schema/struct:Translation2d", "structschema"),
          listOf("/.schema/struct:Pose2d", "structschema"),
          listOf("/Pose", "struct:Pose2d"),
        )

      val data = parsed.records.filterNot { it.isControl }
      data[0].text shouldBe "float64 x; float64 y"
      data[1].text shouldBe "Translation2d translation"
      data[2].payload.size shouldBe 16
      data[2].entryId shouldBe 3
      data[2].timestamp shouldBe 5
      data[2].double shouldBe 1.0
    }

    test("a schema is written once however many entries use it") {
      val schema = structSchema<Unit>("Thing") { float64("x") { 0.0 } }

      val parsed =
        log {
          it.startStructEntry("/one", schema, 0)
          it.startStructArrayEntry("/many", schema, 0)
        }

      parsed.records
        .filter { it.isControl }
        .map { it.startStrings.take(2) } shouldContainExactly
        listOf(
          listOf("/.schema/struct:Thing", "structschema"),
          listOf("/one", "struct:Thing"),
          listOf("/many", "struct:Thing[]"),
        )
    }

    test("struct arrays pack values back to back") {
      val schema = structSchema<Double>("Value") { float64("v") { it } }

      val parsed =
        log {
          val id = it.startStructArrayEntry("/values", schema, 0)
          it.appendStructArray(id, schema, listOf(1.0, 2.0, 3.0), 0)
        }

      val payload = parsed.records.last().payload
      payload.size shouldBe 24
      ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN).getDouble(16) shouldBe 3.0
    }

    test("a different schema under an existing type name is rejected") {
      val a = structSchema<Unit>("Thing") { float64("x") { 0.0 } }
      val b = structSchema<Unit>("Thing") { float32("x") { 0f } }

      log {
        it.startStructEntry("/a", a, 0)
        shouldThrow<IllegalArgumentException> { it.startStructEntry("/b", b, 0) }
      }
    }

    test("a schema that packs fewer bytes than it declares is rejected") {
      val lying =
        object : dev.nextftc.telemetry.struct.StructSchema<Unit> {
          override val typeName = "Lying"
          override val size = 8
          override val schema = "float64 x"

          override fun pack(buffer: ByteBuffer, value: Unit) {}
        }

      log {
        val id = it.startStructEntry("/x", lying, 0)
        shouldThrow<IllegalStateException> { it.appendStruct(id, lying, Unit, 0) }
      }
    }

    test("now is microseconds since the writer was created, on the given time source") {
      val source = TestTimeSource()
      source += 5.seconds
      val writer = WPILogWriter(ByteArrayOutputStream(), timeSource = source)

      writer.now() shouldBe 0
      source += 1500.microseconds
      writer.now() shouldBe 1500
    }

    test("timestampOf converts a mark taken earlier without reading the clock again") {
      val source = TestTimeSource()
      val writer = WPILogWriter(ByteArrayOutputStream(), timeSource = source)
      source += 2.milliseconds
      val mark = source.markNow()
      source += 10.milliseconds

      writer.timestampOf(mark) shouldBe 2_000
      writer.now() shouldBe 12_000
    }

    test("a mark from before the log started is rejected") {
      val source = TestTimeSource()
      val early = source.markNow()
      source += 1.seconds
      val writer = WPILogWriter(ByteArrayOutputStream(), timeSource = source)

      shouldThrow<IllegalArgumentException> { writer.timestampOf(early) }
    }

    test("a mark from a different time source is rejected") {
      val writer = WPILogWriter(ByteArrayOutputStream(), timeSource = TestTimeSource())

      shouldThrow<IllegalArgumentException> { writer.timestampOf(TestTimeSource().markNow()) }
    }

    test("separately created writers keep independent origins on a shared source") {
      val source = TestTimeSource()
      val first = WPILogWriter(ByteArrayOutputStream(), timeSource = source)
      source += 1.seconds
      val second = WPILogWriter(ByteArrayOutputStream(), timeSource = source)
      source += 1.seconds

      first.now() shouldBe 2_000_000
      second.now() shouldBe 1_000_000
    }

    test("omitting the timestamp stamps the current time") {
      val source = TestTimeSource()
      val bytes = ByteArrayOutputStream()
      WPILogWriter(bytes, timeSource = source).use {
        source += 3.milliseconds
        val id = it.startEntry("/d", WPILogType.DOUBLE)
        source += 4.milliseconds
        it.appendDouble(id, 1.0)
        source += 5.milliseconds
        it.finishEntry(id)
      }

      ParsedLog(bytes.toByteArray()).records.map { it.timestamp } shouldContainExactly
        listOf(3_000L, 7_000L, 12_000L)
    }

    test("a mark stamps a value with the moment it was taken") {
      val source = TestTimeSource()
      val bytes = ByteArrayOutputStream()
      WPILogWriter(bytes, timeSource = source).use {
        val id = it.startEntry("/d", WPILogType.DOUBLE, source.markNow())
        source += 2.milliseconds
        val loopStart = source.markNow()
        source += 8.milliseconds
        it.appendDouble(id, 1.0, loopStart)
        it.appendDouble(id, 2.0, loopStart)
      }

      ParsedLog(bytes.toByteArray()).records.map { it.timestamp } shouldContainExactly
        listOf(0L, 2_000L, 2_000L)
    }

    test("struct entries and values accept marks and omitted timestamps") {
      val source = TestTimeSource()
      val schema = structSchema<Double>("Value") { float64("v") { it } }
      val bytes = ByteArrayOutputStream()
      WPILogWriter(bytes, timeSource = source).use {
        source += 1.milliseconds
        val single = it.startStructEntry("/one", schema)
        val many = it.startStructArrayEntry("/many", schema, source.markNow())
        source += 1.milliseconds
        it.appendStruct(single, schema, 1.0)
        it.appendStructArray(many, schema, listOf(1.0, 2.0), source.markNow())
      }

      val records = ParsedLog(bytes.toByteArray()).records
      records.last().timestamp shouldBe 2_000
      records[records.size - 2].timestamp shouldBe 2_000
      records.first().timestamp shouldBe 1_000
    }

    test("each WPILogType has the type string WPILOG readers expect") {
      WPILogType.entries.map { it.typeString } shouldContainExactly
        listOf(
          "raw",
          "boolean",
          "int64",
          "float",
          "double",
          "string",
          "boolean[]",
          "int64[]",
          "float[]",
          "double[]",
          "string[]",
        )
    }

    test("an entry can be started with a WPILogType or with its type string") {
      val source = TestTimeSource()
      val bytes = ByteArrayOutputStream()
      WPILogWriter(bytes, timeSource = source).use {
        it.startEntry("/a", WPILogType.DOUBLE)
        it.startEntry("/b", "double")
        it.startEntry("/c", WPILogType.DOUBLE, source.markNow())
      }

      ParsedLog(bytes.toByteArray()).records.map { it.startStrings[1] } shouldContainExactly
        listOf("double", "double", "double")
    }

    test("an explicit origin is timestamp zero, even if it is before the writer was created") {
      val source = TestTimeSource()
      val origin = source.markNow()
      source += 5.milliseconds
      val writer = WPILogWriter(ByteArrayOutputStream(), timeSource = source, origin = origin)

      writer.now() shouldBe 5_000
      writer.timestampOf(origin) shouldBe 0
    }

    test("two writers with the same source and origin agree on every timestamp") {
      val source = TestTimeSource()
      val origin = source.markNow()
      val first = WPILogWriter(ByteArrayOutputStream(), timeSource = source, origin = origin)
      source += 3.milliseconds
      val second = WPILogWriter(ByteArrayOutputStream(), timeSource = source, origin = origin)
      source += 4.milliseconds
      val mark = source.markNow()

      first.now() shouldBe second.now()
      first.timestampOf(mark) shouldBe 7_000
      second.timestampOf(mark) shouldBe 7_000
    }

    test("a mark before an explicit origin is rejected") {
      val source = TestTimeSource()
      val early = source.markNow()
      source += 1.seconds
      val writer = WPILogWriter(ByteArrayOutputStream(), timeSource = source, origin = source.markNow())

      shouldThrow<IllegalArgumentException> { writer.timestampOf(early) }
    }

    test("an origin from another time source or in the future is rejected") {
      val source = TestTimeSource()

      shouldThrow<IllegalArgumentException> {
        WPILogWriter(
          ByteArrayOutputStream(),
          timeSource = source,
          origin = TestTimeSource().markNow(),
        )
      }
      shouldThrow<IllegalArgumentException> {
        WPILogWriter(
          ByteArrayOutputStream(),
          timeSource = source,
          origin = source.markNow() + 1.seconds,
        )
      }
    }

    test("the default time source is usable") {
      val writer = WPILogWriter(ByteArrayOutputStream())

      (writer.now() >= 0) shouldBe true
    }

    test("writing after close fails, and closing twice is fine") {
      val writer = WPILogWriter(ByteArrayOutputStream())
      val id = writer.startEntry("/a", WPILogType.DOUBLE, 0)
      writer.close()
      writer.close()

      shouldThrow<IllegalStateException> { writer.appendDouble(id, 1.0, 0) }
    }

    test("concurrent writers never tear a record") {
      val bytes = ByteArrayOutputStream()
      val writer = WPILogWriter(bytes)
      val ids = List(4) { writer.startEntry("/t$it", WPILogType.INTEGER, 0) }

      val threads =
        ids.map { id ->
          Thread { repeat(500) { n -> writer.appendInteger(id, n.toLong(), n.toLong()) } }
        }
      threads.forEach { it.start() }
      threads.forEach { it.join() }
      writer.close()

      val data = ParsedLog(bytes.toByteArray()).records.filterNot { it.isControl }
      data.size shouldBe 2000
      for (id in ids) {
        data.filter { it.entryId == id }.map { it.long } shouldContainExactly
          List(500) { it.toLong() }
      }
    }
  })
