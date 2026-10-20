/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.telemetry

import dev.nextftc.telemetry.backend.MockTelemetryBackend
import dev.nextftc.telemetry.struct.StructName
import dev.nextftc.telemetry.struct.structSchema
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.util.Collections
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TestTimeSource

@StructName("Translation2d")
private data class TVec(val x: Double, val y: Double)

@StructName("Pose2d")
private data class TPose(val translation: TVec, val heading: Double)

private class Wrapper(val label: String)

private class DriveState(val left: Double, val right: Double, val pose: TPose) : TelemetryLoggable {
  override fun logTo(table: TelemetryTable) {
    table.log("left", left)
    table.log("right", right)
    table.log("pose", pose)
  }
}

private class Fixture {
  val clock = TestTimeSource()
  val registry = TelemetryRegistry(clock)
  val mock = MockTelemetryBackend().also { registry.register(it) }
  val warnings: MutableList<String> = Collections.synchronizedList(mutableListOf())

  init {
    registry.warningHandler = TelemetryWarningHandler { warnings += it }
  }
}

class TelemetryTableTest :
  FunSpec({
    test("each type logs to a path under the root with its own type") {
      val f = Fixture()
      val table = f.registry.root
      table.log("boolean", true)
      table.log("int", 7)
      table.log("long", 8L)
      table.log("float", 1.5f)
      table.log("double", 2.5)
      table.log("string", "hi")
      table.log("booleans", booleanArrayOf(true, false))
      table.log("ints", intArrayOf(1, 2))
      table.log("longs", longArrayOf(3, 4))
      table.log("floats", floatArrayOf(0.5f))
      table.log("doubles", doubleArrayOf(0.25))

      f.mock.typeAt("/boolean") shouldBe ValueType.BOOLEAN
      f.mock.typeAt("/int") shouldBe ValueType.INTEGER
      f.mock.typeAt("/long") shouldBe ValueType.INTEGER
      f.mock.typeAt("/float") shouldBe ValueType.FLOAT
      f.mock.typeAt("/double") shouldBe ValueType.DOUBLE
      f.mock.typeAt("/string") shouldBe ValueType.STRING
      f.mock.typeAt("/booleans") shouldBe ValueType.BOOLEAN_ARRAY
      f.mock.typeAt("/ints") shouldBe ValueType.INTEGER_ARRAY
      f.mock.typeAt("/longs") shouldBe ValueType.INTEGER_ARRAY
      f.mock.typeAt("/floats") shouldBe ValueType.FLOAT_ARRAY
      f.mock.typeAt("/doubles") shouldBe ValueType.DOUBLE_ARRAY

      f.mock.valuesAt("/boolean") shouldBe listOf(true)
      f.mock.valuesAt("/int") shouldBe listOf(7L)
      f.mock.valuesAt("/long") shouldBe listOf(8L)
      f.mock.valuesAt("/float") shouldBe listOf(1.5f)
      f.mock.valuesAt("/double") shouldBe listOf(2.5)
      f.mock.valuesAt("/string") shouldBe listOf("hi")
      (f.mock.valuesAt("/booleans").single() as BooleanArray).toList() shouldBe listOf(true, false)
      (f.mock.valuesAt("/ints").single() as LongArray).toList() shouldBe listOf(1L, 2L)
      (f.mock.valuesAt("/longs").single() as LongArray).toList() shouldBe listOf(3L, 4L)
      (f.mock.valuesAt("/floats").single() as FloatArray).toList() shouldBe listOf(0.5f)
      (f.mock.valuesAt("/doubles").single() as DoubleArray).toList() shouldBe listOf(0.25)
      f.warnings shouldBe emptyList()
    }

    test("handles log the same as log by name, for every kind") {
      val f = Fixture()
      val table = f.registry.table("T")
      table.booleanTopic("b").set(true)
      table.intTopic("i").set(1)
      table.longTopic("l").set(2L)
      table.floatTopic("f").set(3f)
      table.doubleTopic("d").set(4.0)
      table.stringTopic("s").set("five")
      table.booleanArrayTopic("bs").set(booleanArrayOf(true))
      table.intArrayTopic("is").set(intArrayOf(6))
      table.longArrayTopic("ls").set(longArrayOf(7))
      table.floatArrayTopic("fs").set(floatArrayOf(8f))
      table.doubleArrayTopic("ds").set(doubleArrayOf(9.0))
      table.stringArrayTopic("ss").set(listOf("ten", "eleven"))

      f.mock.valuesAt("/T/b") shouldBe listOf(true)
      f.mock.valuesAt("/T/i") shouldBe listOf(1L)
      f.mock.valuesAt("/T/l") shouldBe listOf(2L)
      f.mock.valuesAt("/T/f") shouldBe listOf(3f)
      f.mock.valuesAt("/T/d") shouldBe listOf(4.0)
      f.mock.valuesAt("/T/s") shouldBe listOf("five")
      f.mock.valuesAt("/T/ss") shouldBe listOf(listOf("ten", "eleven"))
      f.mock.typeAt("/T/ss") shouldBe ValueType.STRING_ARRAY
    }

    test("a boxed value is logged by what it is, and a byte or short as an integer") {
      val f = Fixture()
      val table = f.registry.root
      val boxed: Any = 1.5
      table.log("double", boxed)
      table.log("int", 2 as Any)
      table.log("byte", 3.toByte() as Any)
      table.log("short", 4.toShort() as Any)
      table.log("text", "x" as Any)
      table.log("array", doubleArrayOf(1.0) as Any)

      f.mock.typeAt("/double") shouldBe ValueType.DOUBLE
      f.mock.valuesAt("/int") shouldBe listOf(2L)
      f.mock.valuesAt("/byte") shouldBe listOf(3L)
      f.mock.valuesAt("/short") shouldBe listOf(4L)
      f.mock.typeAt("/text") shouldBe ValueType.STRING
      f.mock.typeAt("/array") shouldBe ValueType.DOUBLE_ARRAY
    }

    test("tables nest, and a name with slashes means the tables on the way") {
      val f = Fixture()
      f.registry.table("Drive").table("Odometry").doubleTopic("x").set(1.0)
      f.registry.root.log("a/b/c", 2.0)
      f.registry.root.log("/leading//and/doubled", 3.0)
      f.registry.table("Drive").log("Odometry/y", 4.0)

      f.mock.paths shouldContainExactly
        listOf("/Drive/Odometry/x", "/a/b/c", "/leading/and/doubled", "/Drive/Odometry/y")
    }

    test("table paths are normalized, and the root is empty") {
      val f = Fixture()

      f.registry.root.path shouldBe ""
      f.registry.table("Drive").path shouldBe "/Drive"
      f.registry.table("/Drive//Odometry/").path shouldBe "/Drive/Odometry"
      f.registry.table("Drive").table("Odometry") shouldBe f.registry.table("Drive/Odometry")
      f.registry.table("") shouldBe f.registry.root
      f.registry.table("Drive").doubleTopic("x").path shouldBe "/Drive/x"
    }

    test("handles and tables are cached") {
      val f = Fixture()
      val table = f.registry.table("T")

      (table.doubleTopic("x") === table.doubleTopic("x")) shouldBe true
      (table.stringTopic("s") === table.stringTopic("s")) shouldBe true
      (f.registry.table("T") === table) shouldBe true
    }

    test("a path is not published until its first value") {
      val f = Fixture()
      val handle = f.registry.table("T").doubleTopic("x")

      f.mock.paths shouldBe emptyList()
      handle.set(1.0)

      f.mock.paths shouldBe listOf("/T/x")
    }

    test("values are stamped in microseconds since the registry's origin") {
      val f = Fixture()
      val handle = f.registry.root.doubleTopic("x")
      handle.set(1.0)
      f.clock += 3.milliseconds
      handle.set(2.0)

      f.mock.timestampsAt("/x") shouldContainExactly listOf(0L, 3_000L)
    }

    test("a value can be stamped with a timestamp or a mark") {
      val f = Fixture()
      f.clock += 10.milliseconds
      val mark = f.clock.markNow()
      f.clock += 5.milliseconds
      val handle = f.registry.root.doubleTopic("x")
      handle.set(1.0, 123L)
      handle.set(2.0, mark)
      handle.set(3.0)

      f.mock.timestampsAt("/x") shouldContainExactly listOf(123L, 10_000L, 15_000L)
    }

    test("every handle kind takes a timestamp and a mark") {
      val f = Fixture()
      f.clock += 1.milliseconds
      val mark = f.clock.markNow()
      val table = f.registry.root
      table.booleanTopic("b").set(true, 5L)
      table.booleanTopic("b2").set(true, mark)
      table.intTopic("i").set(1, 5L)
      table.intTopic("i2").set(1, mark)
      table.longTopic("l").set(1L, 5L)
      table.longTopic("l2").set(1L, mark)
      table.floatTopic("f").set(1f, 5L)
      table.floatTopic("f2").set(1f, mark)
      table.stringTopic("s").set("a", 5L)
      table.stringTopic("s2").set("a", mark)

      for (path in listOf("/b", "/i", "/l", "/f", "/s")) f.mock.timestampsAt(path) shouldBe listOf(5L)
      for (path in listOf("/b2", "/i2", "/l2", "/f2", "/s2")) {
        f.mock.timestampsAt(path) shouldBe listOf(1_000L)
      }
    }

    test("a negative timestamp, or a mark from another clock, warns and drops the value") {
      val f = Fixture()
      val handle = f.registry.root.doubleTopic("x")

      handle.set(1.0, -5L)
      handle.set(2.0, TestTimeSource().markNow())

      f.mock.valuesAt("/x") shouldBe emptyList()
      f.warnings.size shouldBe 2
      f.warnings[0] shouldContain "/x"
    }

    test("a repeated value is skipped, and a changed one is logged") {
      val f = Fixture()
      val handle = f.registry.root.doubleTopic("x")
      for (value in listOf(1.0, 1.0, 2.0, 2.0, 1.0)) handle.set(value)

      f.mock.valuesAt("/x") shouldBe listOf(1.0, 2.0, 1.0)
    }

    test("keepDuplicates logs every value") {
      val f = Fixture()
      val handle = f.registry.root.doubleTopic("x")
      handle.keepDuplicates = true
      repeat(3) { handle.set(1.0) }

      f.mock.valuesAt("/x") shouldBe listOf(1.0, 1.0, 1.0)
    }

    test("every scalar handle skips duplicates") {
      val f = Fixture()
      val table = f.registry.root
      repeat(2) {
        table.booleanTopic("b").set(true)
        table.intTopic("i").set(1)
        table.longTopic("l").set(1L)
        table.floatTopic("f").set(1f)
        table.stringTopic("s").set("a")
      }

      for (path in listOf("/b", "/i", "/l", "/f", "/s")) f.mock.valuesAt(path).size shouldBe 1
    }

    test("NaN counts as a duplicate of itself, and zero is not negative zero") {
      val f = Fixture()
      val handle = f.registry.root.doubleTopic("x")
      handle.set(Double.NaN)
      handle.set(Double.NaN)
      handle.set(0.0)
      handle.set(-0.0)

      f.mock.valuesAt("/x").size shouldBe 3
    }

    test("arrays and structs are logged every time, since they can change in place") {
      val f = Fixture()
      val array = doubleArrayOf(1.0)
      val handle = f.registry.root.doubleArrayTopic("a")
      handle.set(array)
      array[0] = 2.0
      handle.set(array)
      val pose = TPose(TVec(1.0, 2.0), 0.0)
      f.registry.root.log("pose", pose)
      f.registry.root.log("pose", pose)

      (f.mock.valuesAt("/a")[0] as DoubleArray).toList() shouldBe listOf(1.0)
      (f.mock.valuesAt("/a")[1] as DoubleArray).toList() shouldBe listOf(2.0)
      f.mock.valuesAt("/pose").size shouldBe 2
    }

    test("a path that holds one type warns and drops a different one") {
      val f = Fixture()
      f.registry.root.log("x", 1.0)
      f.registry.root.log("x", "text")
      f.registry.root.log("x", "more text")

      f.mock.valuesAt("/x") shouldBe listOf(1.0)
      f.warnings.size shouldBe 1
      f.warnings[0] shouldContain "/x"
      f.warnings[0] shouldContain "double"
      f.warnings[0] shouldContain "string"
    }

    test("a handle of the wrong type drops its values without disturbing the right one") {
      val f = Fixture()
      val table = f.registry.root
      val right = table.doubleTopic("x")
      val wrong = table.stringTopic("x")
      right.set(1.0)
      wrong.set("ignored")
      right.set(2.0)

      f.mock.valuesAt("/x") shouldBe listOf(1.0, 2.0)
      f.warnings.size shouldBe 1
    }

    test("an int handle and a long handle on the same path both log") {
      val f = Fixture()
      val table = f.registry.root
      table.intTopic("n").set(1)
      table.longTopic("n").set(2L)

      f.mock.valuesAt("/n") shouldBe listOf(1L, 2L)
      f.warnings shouldBe emptyList()
    }

    test("an empty name warns and drops the value") {
      val f = Fixture()
      f.registry.root.log("", 1.0)
      f.registry.table("T").log("a/", 2.0)

      f.mock.paths shouldBe emptyList()
      f.warnings.size shouldBe 2
    }

    test("a class is logged as a struct reflected from its type") {
      val f = Fixture()
      val pose = TPose(TVec(1.0, 2.0), 0.5)
      f.registry.root.log("pose", pose)

      val schema = f.registry.structs.schemaOf<TPose>()
      f.mock.typeAt("/pose") shouldBe StructType(schema)
      f.mock.valuesAt("/pose") shouldBe listOf(pose)
      schema.schema shouldBe "Translation2d translation; float64 heading"
    }

    test("a struct handle comes from a class, a reified type, or a schema") {
      val f = Fixture()
      val table = f.registry.root
      val pose = TPose(TVec(1.0, 2.0), 0.5)
      table.topic<TPose>("reified").set(pose)
      table.topic("byClass", TPose::class).set(pose)
      table.topic("byJavaClass", TPose::class.java).set(pose)
      val vec = structSchema<TVec>("Translation2d") {
        float64("x") { it.x }
        float64("y") { it.y }
      }
      table.topic("bySchema", vec).set(TVec(3.0, 4.0))

      f.mock.valuesAt("/reified") shouldBe listOf(pose)
      f.mock.valuesAt("/byClass") shouldBe listOf(pose)
      f.mock.valuesAt("/byJavaClass") shouldBe listOf(pose)
      f.mock.valuesAt("/bySchema") shouldBe listOf(TVec(3.0, 4.0))
      f.mock.typeAt("/bySchema") shouldBe StructType(vec)
    }

    test("a list of structs is logged with its element type, from a handle or the extension") {
      val f = Fixture()
      val table = f.registry.root
      val path = listOf(TVec(1.0, 2.0), TVec(3.0, 4.0))
      table.logArray("extension", path)
      table.arrayTopic<TVec>("handle").set(path)
      table.arrayTopic("byClass", TVec::class).set(path)

      val schema = f.registry.structs.schemaOf<TVec>()
      f.mock.typeAt("/extension") shouldBe StructType(schema, isArray = true)
      f.mock.valuesAt("/extension") shouldBe listOf(path)
      f.mock.valuesAt("/handle") shouldBe listOf(path)
      f.mock.valuesAt("/byClass") shouldBe listOf(path)
    }

    test("an array of objects is logged as an array of structs, or of strings") {
      val f = Fixture()
      f.registry.root.log("vecs", arrayOf(TVec(1.0, 2.0)))
      f.registry.root.log("names", arrayOf("a", "b"))

      f.mock.typeAt("/vecs") shouldBe StructType(f.registry.structs.schemaOf<TVec>(), isArray = true)
      f.mock.valuesAt("/vecs") shouldBe listOf(listOf(TVec(1.0, 2.0)))
      f.mock.typeAt("/names") shouldBe ValueType.STRING_ARRAY
      f.mock.valuesAt("/names") shouldBe listOf(listOf("a", "b"))
    }

    test("a list or a map passed to log is rejected with a pointer to arrayTopic") {
      val f = Fixture()
      f.registry.root.log("list", listOf(1.0) as Any)
      f.registry.root.log("map", mapOf("a" to 1.0) as Any)

      f.mock.paths shouldBe emptyList()
      f.warnings.size shouldBe 2
      f.warnings[0] shouldContain "arrayTopic"
    }

    test("a class that can't be a struct warns, and its handle drops every value") {
      val f = Fixture()
      f.registry.root.log("wrapper", Wrapper("text"))
      val handle = f.registry.root.topic<Wrapper>("handle")
      handle.set(Wrapper("text"))

      f.mock.paths shouldBe emptyList()
      f.warnings.size shouldBe 2
      f.warnings[0] shouldContain "Wrapper.label"
    }

    test("a loggable is logged into the table named by the caller") {
      val f = Fixture()
      val pose = TPose(TVec(1.0, 2.0), 0.5)
      f.registry.root.log("drive", DriveState(1.0, 2.0, pose))

      f.mock.paths shouldContainExactly listOf("/drive/left", "/drive/right", "/drive/pose")
      f.mock.valuesAt("/drive/left") shouldBe listOf(1.0)
      f.mock.valuesAt("/drive/pose") shouldBe listOf(pose)
    }

    test("a lambda is a loggable") {
      val f = Fixture()
      val loggable = TelemetryLoggable { it.log("value", 5) }
      f.registry.table("T").log("thing", loggable as Any)

      f.mock.valuesAt("/T/thing/value") shouldBe listOf(5L)
    }

    test("arrays given to a handle are copied, so changing one later changes nothing") {
      val f = Fixture()
      val array = doubleArrayOf(1.0, 2.0)
      f.registry.root.doubleArrayTopic("a").set(array)
      array[0] = 99.0

      (f.mock.valuesAt("/a").single() as DoubleArray).toList() shouldBe listOf(1.0, 2.0)
    }

    test("an int array is widened to integers") {
      val f = Fixture()
      f.registry.root.intArrayTopic("a").set(intArrayOf(Int.MAX_VALUE, -1))

      (f.mock.valuesAt("/a").single() as LongArray).toList() shouldBe
        listOf(Int.MAX_VALUE.toLong(), -1L)
    }

    test("struct types are equal when their schemas and array-ness are") {
      val one = structSchema<TVec>("T") { float64("x") { it.x } }
      val same = structSchema<TVec>("T") { float64("x") { it.x } }
      val other = structSchema<TVec>("T") { float32("x") { it.x.toFloat() } }

      StructType(one) shouldBe StructType(same)
      StructType(one).hashCode() shouldBe StructType(same).hashCode()
      (StructType(one) == StructType(other)) shouldBe false
      (StructType(one) == StructType(one, isArray = true)) shouldBe false
      StructType(one).toString() shouldBe "struct:T"
      StructType(one, isArray = true).toString() shouldBe "struct:T[]"
      ValueType.DOUBLE_ARRAY.toString() shouldBe "double array"
    }
  })
