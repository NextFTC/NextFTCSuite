/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.telemetry.wpilog

import dev.nextftc.telemetry.struct.StructSchema
import java.io.BufferedOutputStream
import java.io.Closeable
import java.io.Flushable
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.time.ComparableTimeMark
import kotlin.time.TimeSource

/**
 * The WPILOG types for the non-struct values [WPILogWriter] can append.
 *
 * @property typeString The type's name in the log, which is what readers see
 */
enum class WPILogType(val typeString: String) {
  RAW("raw"),
  BOOLEAN("boolean"),
  INTEGER("int64"),
  FLOAT("float"),
  DOUBLE("double"),
  STRING("string"),
  BOOLEAN_ARRAY("boolean[]"),
  INTEGER_ARRAY("int64[]"),
  FLOAT_ARRAY("float[]"),
  DOUBLE_ARRAY("double[]"),
  STRING_ARRAY("string[]"),
}

/**
 * Writes the [WPILOG](https://github.com/wpilibsuite/allwpilib/blob/main/wpiutil/doc/datalog.adoc)
 * format, version 1.0, to [output].
 *
 * Start an entry with [startEntry] (or [startStructEntry]), then append values to the id it returns.
 * Every timestamp is in microseconds since [origin], as measured by [timeSource]. Get one from [now]
 * or [timestampOf], or compute your own on the same basis.
 *
 * All methods are safe to call from multiple threads. Each call writes one whole record, so records
 * from different threads interleave but never tear. The writer adds a buffer in front of [output]
 * unless it is already a [BufferedOutputStream], so call [flush] to force pending records out.
 *
 * @param output Where the log goes. [close] closes it.
 * @param extraHeader A string stored in the file header, conventionally JSON. Readers ignore it.
 * @param timeSource The clock that timestamps are measured on. A log created on its own can use the
 *   default. To line a log up with a [dev.nextftc.telemetry.nt.NT4Server] or another writer, give
 *   them the same source and the same [origin].
 * @param origin The moment timestamp zero refers to, a mark from [timeSource]. The default is the
 *   moment the writer is created. Share one origin between sinks that should agree on every
 *   timestamp.
 * @throws IllegalArgumentException if [origin] isn't from [timeSource], or is in the future
 */
class WPILogWriter
@JvmOverloads
constructor(
  output: OutputStream,
  extraHeader: String = "",
  private val timeSource: TimeSource.WithComparableMarks = TimeSource.Monotonic,
  val origin: ComparableTimeMark = timeSource.markNow(),
) : Closeable,
  Flushable {
  init {
    // Subtracting marks from different sources throws, which checks that they match.
    require(!(timeSource.markNow() - origin).isNegative()) { "The origin is in the future" }
  }

  private val out: OutputStream =
    output as? BufferedOutputStream ?: BufferedOutputStream(output, BUFFER_SIZE)
  private val header = ByteArray(MAX_RECORD_HEADER_SIZE)
  private var scratch = ByteBuffer.allocate(INITIAL_SCRATCH_SIZE).order(ByteOrder.LITTLE_ENDIAN)
  private var nextEntryId = 1
  private var closed = false

  /** Schema text by struct type name, for the schema entries already written. */
  private val writtenSchemas = HashMap<String, String>()

  init {
    val extra = extraHeader.toByteArray(Charsets.UTF_8)
    out.write(MAGIC)
    // Version 1.0, as a little-endian 16-bit value: minor byte first, then major.
    out.write(0x00)
    out.write(0x01)
    out.write(littleEndianInt(extra.size))
    out.write(extra)
  }

  // region Timestamps

  /** Returns the current time as a timestamp in microseconds since [origin]. */
  fun now(): Long = timestampOf(timeSource.markNow())

  /**
   * Returns [mark] as a timestamp in microseconds since [origin].
   *
   * Use this to stamp several values with one moment, such as the start of a loop iteration.
   *
   * @param mark A mark from this writer's [timeSource]
   * @throws IllegalArgumentException if [mark] is from a different time source, or is earlier than
   *   [origin]
   */
  fun timestampOf(mark: ComparableTimeMark): Long {
    val micros = (mark - origin).inWholeMicroseconds
    require(micros >= 0) { "The mark is $micros microseconds before the origin" }
    return micros
  }

  // endregion

  // region Entries

  /**
   * Starts an entry and returns its id.
   *
   * @param name The entry name, such as `/Drive/Pose`.
   * @param type The entry type string, such as `"json"`. Prefer the overload that takes a
   *   [WPILogType] for the types that have one.
   * @param metadata Free-form metadata, conventionally JSON.
   */
  @Synchronized
  @JvmOverloads
  fun startEntry(name: String, type: String, timestampMicros: Long = now(), metadata: String = ""): Int {
    val id = nextEntryId++
    val nameBytes = name.toByteArray(Charsets.UTF_8)
    val typeBytes = type.toByteArray(Charsets.UTF_8)
    val metadataBytes = metadata.toByteArray(Charsets.UTF_8)
    val buffer =
      reserve(1 + 4 + (4 + nameBytes.size) + (4 + typeBytes.size) + (4 + metadataBytes.size))
    buffer.put(CONTROL_START)
    buffer.putInt(id)
    buffer.putString(nameBytes)
    buffer.putString(typeBytes)
    buffer.putString(metadataBytes)
    emit(CONTROL_ENTRY_ID, timestampMicros, buffer)
    return id
  }

  /** Starts an entry of [type]. See the overload that takes a type string. */
  @JvmOverloads
  fun startEntry(
    name: String,
    type: WPILogType,
    timestampMicros: Long = now(),
    metadata: String = "",
  ): Int = startEntry(name, type.typeString, timestampMicros, metadata)

  /** Marks the entry [id] as finished. No more values should be appended to it. */
  @Synchronized
  @JvmOverloads
  fun finishEntry(id: Int, timestampMicros: Long = now()) {
    val buffer = reserve(1 + 4)
    buffer.put(CONTROL_FINISH)
    buffer.putInt(id)
    emit(CONTROL_ENTRY_ID, timestampMicros, buffer)
  }

  /** Replaces the metadata of the entry [id]. */
  @Synchronized
  @JvmOverloads
  fun setMetadata(id: Int, metadata: String, timestampMicros: Long = now()) {
    val metadataBytes = metadata.toByteArray(Charsets.UTF_8)
    val buffer = reserve(1 + 4 + 4 + metadataBytes.size)
    buffer.put(CONTROL_SET_METADATA)
    buffer.putInt(id)
    buffer.putString(metadataBytes)
    emit(CONTROL_ENTRY_ID, timestampMicros, buffer)
  }

  // endregion

  // region Values

  /** Appends [value] to the entry [id] as is. Use this for `raw` entries. */
  @Synchronized
  @JvmOverloads
  fun appendRaw(id: Int, value: ByteArray, timestampMicros: Long = now()) {
    writeRecord(id, timestampMicros, value, value.size)
  }

  @Synchronized
  @JvmOverloads
  fun appendBoolean(id: Int, value: Boolean, timestampMicros: Long = now()) {
    val buffer = reserve(1)
    buffer.put(if (value) 1 else 0)
    emit(id, timestampMicros, buffer)
  }

  @Synchronized
  @JvmOverloads
  fun appendInteger(id: Int, value: Long, timestampMicros: Long = now()) {
    val buffer = reserve(Long.SIZE_BYTES)
    buffer.putLong(value)
    emit(id, timestampMicros, buffer)
  }

  @Synchronized
  @JvmOverloads
  fun appendFloat(id: Int, value: Float, timestampMicros: Long = now()) {
    val buffer = reserve(Float.SIZE_BYTES)
    buffer.putFloat(value)
    emit(id, timestampMicros, buffer)
  }

  @Synchronized
  @JvmOverloads
  fun appendDouble(id: Int, value: Double, timestampMicros: Long = now()) {
    val buffer = reserve(Double.SIZE_BYTES)
    buffer.putDouble(value)
    emit(id, timestampMicros, buffer)
  }

  /** Appends [value] as UTF-8. */
  @Synchronized
  @JvmOverloads
  fun appendString(id: Int, value: String, timestampMicros: Long = now()) {
    val bytes = value.toByteArray(Charsets.UTF_8)
    writeRecord(id, timestampMicros, bytes, bytes.size)
  }

  @Synchronized
  @JvmOverloads
  fun appendBooleanArray(id: Int, value: BooleanArray, timestampMicros: Long = now()) {
    val buffer = reserve(value.size)
    for (element in value) buffer.put(if (element) 1 else 0)
    emit(id, timestampMicros, buffer)
  }

  @Synchronized
  @JvmOverloads
  fun appendIntegerArray(id: Int, value: LongArray, timestampMicros: Long = now()) {
    val buffer = reserve(value.size * Long.SIZE_BYTES)
    for (element in value) buffer.putLong(element)
    emit(id, timestampMicros, buffer)
  }

  @Synchronized
  @JvmOverloads
  fun appendFloatArray(id: Int, value: FloatArray, timestampMicros: Long = now()) {
    val buffer = reserve(value.size * Float.SIZE_BYTES)
    for (element in value) buffer.putFloat(element)
    emit(id, timestampMicros, buffer)
  }

  @Synchronized
  @JvmOverloads
  fun appendDoubleArray(id: Int, value: DoubleArray, timestampMicros: Long = now()) {
    val buffer = reserve(value.size * Double.SIZE_BYTES)
    for (element in value) buffer.putDouble(element)
    emit(id, timestampMicros, buffer)
  }

  @Synchronized
  @JvmOverloads
  fun appendStringArray(id: Int, value: List<String>, timestampMicros: Long = now()) {
    val encoded = value.map { it.toByteArray(Charsets.UTF_8) }
    val buffer = reserve(4 + encoded.sumOf { 4 + it.size })
    buffer.putInt(encoded.size)
    for (bytes in encoded) buffer.putString(bytes)
    emit(id, timestampMicros, buffer)
  }

  // endregion

  // region Structs

  /**
   * Starts an entry that holds single values of [schema], and returns its id.
   *
   * Writes the schema entries for [schema] and every struct type nested in it first, if they have not
   * been written already, so a reader can decode the first value.
   *
   * @throws IllegalArgumentException if a different schema with the same type name was already
   *   written
   */
  @Synchronized
  @JvmOverloads
  fun startStructEntry(
    name: String,
    schema: StructSchema<*>,
    timestampMicros: Long = now(),
    metadata: String = "",
  ): Int {
    writeSchemas(schema, timestampMicros)
    return startEntry(name, schema.typeString, timestampMicros, metadata)
  }

  /**
   * Starts an entry that holds arrays of [schema] values, and returns its id. Otherwise the same as
   * [startStructEntry].
   */
  @Synchronized
  @JvmOverloads
  fun startStructArrayEntry(
    name: String,
    schema: StructSchema<*>,
    timestampMicros: Long = now(),
    metadata: String = "",
  ): Int {
    writeSchemas(schema, timestampMicros)
    return startEntry(name, schema.arrayTypeString, timestampMicros, metadata)
  }

  /** Appends one packed [value] to an entry started with [startStructEntry]. */
  @Synchronized
  @JvmOverloads
  fun <T> appendStruct(id: Int, schema: StructSchema<T>, value: T, timestampMicros: Long = now()) {
    val buffer = reserve(schema.size)
    schema.pack(buffer, value)
    checkPacked(schema, buffer, schema.size)
    emit(id, timestampMicros, buffer)
  }

  /** Appends [values] packed back to back to an entry started with [startStructArrayEntry]. */
  @Synchronized
  @JvmOverloads
  fun <T> appendStructArray(
    id: Int,
    schema: StructSchema<T>,
    values: List<T>,
    timestampMicros: Long = now(),
  ) {
    val buffer = reserve(schema.size * values.size)
    for (value in values) schema.pack(buffer, value)
    checkPacked(schema, buffer, schema.size * values.size)
    emit(id, timestampMicros, buffer)
  }

  private fun checkPacked(schema: StructSchema<*>, buffer: ByteBuffer, expected: Int) {
    check(buffer.position() == expected) {
      "Struct '${schema.typeName}' declares a size of ${schema.size} but packed " +
        "${buffer.position()} bytes for $expected expected"
    }
  }

  /** Writes the schema entries for [schema], nested types first, skipping those already written. */
  private fun writeSchemas(schema: StructSchema<*>, timestampMicros: Long) {
    for (nested in schema.nested) writeSchemas(nested, timestampMicros)
    val existing = writtenSchemas[schema.typeName]
    if (existing != null) {
      require(existing == schema.schema) {
        "Struct '${schema.typeName}' was already written with a different schema: " +
          "'$existing', not '${schema.schema}'"
      }
      return
    }
    val id =
      startEntry(schema.schemaTopicName, StructSchema.SCHEMA_ENTRY_TYPE, timestampMicros)
    appendString(id, schema.schema, timestampMicros)
    writtenSchemas[schema.typeName] = schema.schema
  }

  // endregion

  // region Overloads that take a mark

  /** Like [startEntry], stamped with [at]. See [timestampOf]. */
  fun startEntry(name: String, type: String, at: ComparableTimeMark, metadata: String = ""): Int =
    startEntry(name, type, timestampOf(at), metadata)

  /** Like [startEntry], for a [WPILogType] and stamped with [at]. See [timestampOf]. */
  fun startEntry(name: String, type: WPILogType, at: ComparableTimeMark, metadata: String = ""): Int =
    startEntry(name, type.typeString, timestampOf(at), metadata)

  /** Like [finishEntry], stamped with [at]. See [timestampOf]. */
  fun finishEntry(id: Int, at: ComparableTimeMark) = finishEntry(id, timestampOf(at))

  /** Like [setMetadata], stamped with [at]. See [timestampOf]. */
  fun setMetadata(id: Int, metadata: String, at: ComparableTimeMark) =
    setMetadata(id, metadata, timestampOf(at))

  /** Like [appendRaw], stamped with [at]. See [timestampOf]. */
  fun appendRaw(id: Int, value: ByteArray, at: ComparableTimeMark) = appendRaw(id, value, timestampOf(at))

  /** Like [appendBoolean], stamped with [at]. See [timestampOf]. */
  fun appendBoolean(id: Int, value: Boolean, at: ComparableTimeMark) =
    appendBoolean(id, value, timestampOf(at))

  /** Like [appendInteger], stamped with [at]. See [timestampOf]. */
  fun appendInteger(id: Int, value: Long, at: ComparableTimeMark) =
    appendInteger(id, value, timestampOf(at))

  /** Like [appendFloat], stamped with [at]. See [timestampOf]. */
  fun appendFloat(id: Int, value: Float, at: ComparableTimeMark) = appendFloat(id, value, timestampOf(at))

  /** Like [appendDouble], stamped with [at]. See [timestampOf]. */
  fun appendDouble(id: Int, value: Double, at: ComparableTimeMark) =
    appendDouble(id, value, timestampOf(at))

  /** Like [appendString], stamped with [at]. See [timestampOf]. */
  fun appendString(id: Int, value: String, at: ComparableTimeMark) =
    appendString(id, value, timestampOf(at))

  /** Like [appendBooleanArray], stamped with [at]. See [timestampOf]. */
  fun appendBooleanArray(id: Int, value: BooleanArray, at: ComparableTimeMark) =
    appendBooleanArray(id, value, timestampOf(at))

  /** Like [appendIntegerArray], stamped with [at]. See [timestampOf]. */
  fun appendIntegerArray(id: Int, value: LongArray, at: ComparableTimeMark) =
    appendIntegerArray(id, value, timestampOf(at))

  /** Like [appendFloatArray], stamped with [at]. See [timestampOf]. */
  fun appendFloatArray(id: Int, value: FloatArray, at: ComparableTimeMark) =
    appendFloatArray(id, value, timestampOf(at))

  /** Like [appendDoubleArray], stamped with [at]. See [timestampOf]. */
  fun appendDoubleArray(id: Int, value: DoubleArray, at: ComparableTimeMark) =
    appendDoubleArray(id, value, timestampOf(at))

  /** Like [appendStringArray], stamped with [at]. See [timestampOf]. */
  fun appendStringArray(id: Int, value: List<String>, at: ComparableTimeMark) =
    appendStringArray(id, value, timestampOf(at))

  /** Like [startStructEntry], stamped with [at]. See [timestampOf]. */
  fun startStructEntry(
    name: String,
    schema: StructSchema<*>,
    at: ComparableTimeMark,
    metadata: String = "",
  ): Int = startStructEntry(name, schema, timestampOf(at), metadata)

  /** Like [startStructArrayEntry], stamped with [at]. See [timestampOf]. */
  fun startStructArrayEntry(
    name: String,
    schema: StructSchema<*>,
    at: ComparableTimeMark,
    metadata: String = "",
  ): Int = startStructArrayEntry(name, schema, timestampOf(at), metadata)

  /** Like [appendStruct], stamped with [at]. See [timestampOf]. */
  fun <T> appendStruct(id: Int, schema: StructSchema<T>, value: T, at: ComparableTimeMark) =
    appendStruct(id, schema, value, timestampOf(at))

  /** Like [appendStructArray], stamped with [at]. See [timestampOf]. */
  fun <T> appendStructArray(id: Int, schema: StructSchema<T>, values: List<T>, at: ComparableTimeMark) =
    appendStructArray(id, schema, values, timestampOf(at))

  // endregion

  @Synchronized
  override fun flush() {
    out.flush()
  }

  /** Flushes pending records and closes the underlying stream. Does nothing if already closed. */
  @Synchronized
  override fun close() {
    if (closed) return
    closed = true
    out.close()
  }

  /**
   * Returns the scratch buffer, cleared and with room for at least [size] bytes. It is little-endian,
   * and the caller fills it and passes it to [emit].
   */
  private fun reserve(size: Int): ByteBuffer {
    check(!closed) { "The WPILOG writer is closed" }
    if (scratch.capacity() < size) {
      scratch =
        ByteBuffer.allocate(maxOf(size, scratch.capacity() * 2)).order(ByteOrder.LITTLE_ENDIAN)
    }
    scratch.clear()
    return scratch
  }

  private fun emit(entryId: Int, timestampMicros: Long, buffer: ByteBuffer) {
    writeRecord(entryId, timestampMicros, buffer.array(), buffer.position())
  }

  private fun writeRecord(entryId: Int, timestamp: Long, payload: ByteArray, length: Int) {
    check(!closed) { "The WPILOG writer is closed" }
    val idLength = bytesNeeded(entryId.toLong() and 0xFFFFFFFFL)
    val sizeLength = bytesNeeded(length.toLong())
    val timestampLength = bytesNeeded(timestamp)

    header[0] = ((idLength - 1) or ((sizeLength - 1) shl 2) or ((timestampLength - 1) shl 4)).toByte()
    var position = 1
    position = putLittleEndian(position, entryId.toLong() and 0xFFFFFFFFL, idLength)
    position = putLittleEndian(position, length.toLong(), sizeLength)
    position = putLittleEndian(position, timestamp, timestampLength)

    out.write(header, 0, position)
    out.write(payload, 0, length)
  }

  private fun putLittleEndian(position: Int, value: Long, length: Int): Int {
    var remaining = value
    for (i in 0 until length) {
      header[position + i] = remaining.toByte()
      remaining = remaining ushr 8
    }
    return position + length
  }

  private fun ByteBuffer.putString(bytes: ByteArray) {
    putInt(bytes.size)
    put(bytes)
  }

  private companion object {
    val MAGIC = "WPILOG".toByteArray(Charsets.US_ASCII)

    const val BUFFER_SIZE = 64 * 1024
    const val INITIAL_SCRATCH_SIZE = 256

    /** One bitfield byte, then up to 4 bytes of entry id, 4 of payload size, and 8 of timestamp. */
    const val MAX_RECORD_HEADER_SIZE = 1 + 4 + 4 + 8

    /** Control records are written to entry id 0. */
    const val CONTROL_ENTRY_ID = 0
    const val CONTROL_START: Byte = 0
    const val CONTROL_FINISH: Byte = 1
    const val CONTROL_SET_METADATA: Byte = 2

    /** The fewest bytes, from 1 to 8, that hold [value] as an unsigned integer. A negative value needs all 8. */
    fun bytesNeeded(value: Long): Int =
      maxOf(1, (Long.SIZE_BITS - java.lang.Long.numberOfLeadingZeros(value) + 7) / 8)

    fun littleEndianInt(value: Int): ByteArray =
      ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array()
  }
}
