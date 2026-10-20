/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.telemetry.nt

import dev.nextftc.telemetry.struct.StructSchema
import dev.nextftc.telemetry.toJsonObject
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.install
import io.ktor.server.application.serverConfig
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.response.header
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readBytes
import io.ktor.websocket.readText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.put
import org.msgpack.core.MessageBufferPacker
import org.msgpack.core.MessageFormat
import org.msgpack.core.MessagePack
import org.msgpack.core.MessagePackException
import org.msgpack.core.MessageUnpacker
import org.msgpack.value.ValueType
import java.io.Closeable
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.time.ComparableTimeMark
import kotlin.time.TimeSource

/**
 * The NetworkTables 4 types for the values [NT4Server] can publish.
 *
 * @property typeString The type's name in `announce` messages, which is also what clients see
 * @property id The type's number in binary value updates
 */
enum class NT4Type(val typeString: String, internal val id: Int) {
  BOOLEAN("boolean", 0),
  INTEGER("int", 2),
  FLOAT("float", 3),
  DOUBLE("double", 1),
  STRING("string", 4),
  RAW("raw", 5),
  BOOLEAN_ARRAY("boolean[]", 16),
  INTEGER_ARRAY("int[]", 18),
  FLOAT_ARRAY("float[]", 19),
  DOUBLE_ARRAY("double[]", 17),
  STRING_ARRAY("string[]", 20),

  /** The type of the topics that hold struct schemas, which are binary. */
  STRUCT_SCHEMA(StructSchema.SCHEMA_ENTRY_TYPE, 5),
  ;

  internal companion object {
    /**
     * Returns the number that binary updates use for the type string [typeString]. A type that isn't
     * one of the entries, including `msgpack` and struct types, travels as binary, except `json`,
     * which is text.
     */
    fun idOf(typeString: String): Int = entries.firstOrNull { it.typeString == typeString }?.id
      ?: if (typeString == "json") STRING.id else RAW.id
  }
}

/**
 * A [NetworkTables 4](https://github.com/wpilibsuite/allwpilib/blob/main/ntcore/doc/networktables4.adoc)
 * server that publishes values to dashboards such as AdvantageScope, Glass and Shuffleboard.
 *
 * Publish a topic with [publish], [publishStruct] or [publishStructArray], which return its id, and
 * then set values on it with the `set*` methods. A client that connects and subscribes is told about
 * the topics it asked for and is sent their current values, and then every change after that.
 *
 * ```kotlin
 * val server = NT4Server()
 * server.start()
 * val voltage = server.publish("/Robot/Battery", NT4Type.DOUBLE)
 * server.setDouble(voltage, 12.4)
 * ```
 *
 * This is a publish-only server. It ignores `publish`, `unpublish` and `setproperties` messages, and
 * values clients try to publish, so clients can read but not write. It sends every change to a
 * subscriber, whatever the subscription's `periodic` and `all` options say, and it has no meta
 * topics such as `$clients`. It answers the clock-synchronization messages that clients use to
 * convert its timestamps.
 *
 * Every timestamp is in microseconds since [origin], as measured by [timeSource]. That is also the
 * time base that clients synchronize to, so a log writer that stamps values with the same source
 * and origin agrees with what dashboards see.
 *
 * A client that can't keep up is disconnected, so it can reconnect and catch up, rather than
 * making the robot wait or run out of memory.
 *
 * All methods are safe to call from multiple threads, and the `set*` methods never block on the
 * network.
 *
 * @param port The port to listen on. The standard NetworkTables 4 port is [DEFAULT_PORT], and 0
 *   picks a free one, which [boundPort] reports after [start].
 * @param host The address to listen on. The default is every network interface.
 * @param timeSource The clock that timestamps are measured on. A server created on its own can use
 *   the default. To line it up with a [dev.nextftc.telemetry.wpilog.WPILogWriter], give them the
 *   same source and the same [origin].
 * @param origin The moment timestamp zero refers to, a mark from [timeSource]. The default is the
 *   moment the server is created. Share one origin between sinks that should agree on every
 *   timestamp.
 * @throws IllegalArgumentException if [origin] isn't from [timeSource], or is in the future
 */
class NT4Server
@JvmOverloads
constructor(
  val port: Int = DEFAULT_PORT,
  val host: String = "0.0.0.0",
  private val timeSource: TimeSource.WithComparableMarks = TimeSource.Monotonic,
  val origin: ComparableTimeMark = timeSource.markNow(),
) : Closeable {
  init {
    // Subtracting marks from different sources throws, which checks that they match.
    require(!(timeSource.markNow() - origin).isNegative()) { "The origin is in the future" }
  }

  /** Guards the topics, the clients, and everything about a client's subscriptions. */
  private val lock = Any()
  private val topicsById = LinkedHashMap<Int, Topic>()
  private val topicsByName = HashMap<String, Topic>()
  private val clients = HashSet<Client>()
  private val writer = MessagePack.newDefaultBufferPacker()
  private var nextTopicId = 0

  /** Schema text by struct type name, for the schema topics already published. */
  private val publishedSchemas = HashMap<String, String>()

  /**
   * The type of every name that has ever been published, including the ones since removed.
   *
   * Dashboards such as AdvantageScope key a field by its name for the whole time they are connected,
   * and keep the first type they see. A name that came back with another type would show up there
   * as a "Conflicting types" error, so it is refused here instead.
   */
  private val typeEverPublished = HashMap<String, String>()

  private val lifecycle = Any()
  private var engine: EmbeddedServer<*, *>? = null

  /** Whether [start] has been called without a [stop] since. */
  val isRunning: Boolean
    get() = synchronized(lifecycle) { engine != null }

  /** How many dashboards are connected. Connections that only measure round-trip time aren't counted. */
  val clientCount: Int
    get() = synchronized(lock) { clients.size }

  /**
   * The port the server is listening on, which differs from [port] when that is 0.
   *
   * @throws IllegalStateException if the server isn't running
   */
  val boundPort: Int
    get() {
      val running = synchronized(lifecycle) { checkNotNull(engine) { "The NT4 server isn't running" } }
      return runBlocking { running.engine.resolvedConnectors().first().port }
    }

  // region Lifecycle

  /**
   * Starts listening, without blocking, and returns once the port is bound.
   *
   * @throws IllegalStateException if the server is already running
   * @throws java.net.BindException if the port is taken
   */
  fun start() {
    synchronized(lifecycle) {
      check(engine == null) { "The NT4 server is already running" }
      val server =
        embeddedServer(
          CIO,
          serverConfig {
            module {
              install(WebSockets) {
                maxFrameSize = Long.MAX_VALUE
                masking = false
              }
              routing {
                // The version 4.1 protocol goes first so it wins when a client offers several.
                webSocket("/nt/{name}", protocol = PROTOCOL_V4_1) {
                  serve(Protocol.V4_1)
                }
                webSocket("/nt/{name}", protocol = PROTOCOL_V4_0) {
                  serve(Protocol.V4_0)
                }
                webSocket("/nt/{name}", protocol = PROTOCOL_RTT) {
                  serve(Protocol.RTT)
                }
                // Plain HTTP requests are how browser-based dashboards check that the server is there.
                get("/") { respondAlive() }
                get("/nt/{name}") { respondAlive() }
              }
            }
          },
          configure = {
            connector {
              port = this@NT4Server.port
              host = this@NT4Server.host
            }
            // So the server can restart on its port at once, as when the Robot Controller restarts.
            reuseAddress = true
          },
        )
      server.start(wait = false)
      engine = server
    }
  }

  /** Stops listening and disconnects every client. Topics and their values are kept. */
  fun stop() {
    val stopping = synchronized(lifecycle) { engine.also { engine = null } } ?: return
    stopping.stop(gracePeriodMillis = 100, timeoutMillis = 1_000)
  }

  override fun close() = stop()

  private suspend fun io.ktor.server.routing.RoutingContext.respondAlive() {
    call.response.header("Access-Control-Allow-Origin", "*")
    call.respondText("", status = HttpStatusCode.OK)
  }

  // endregion

  // region Timestamps

  /** Returns the current time as a timestamp in microseconds since [origin]. */
  fun now(): Long = timestampOf(timeSource.markNow())

  /**
   * Returns [mark] as a timestamp in microseconds since [origin].
   *
   * @param mark A mark from this server's time source
   * @throws IllegalArgumentException if [mark] is from a different time source, or is earlier than
   *   [origin]
   */
  fun timestampOf(mark: ComparableTimeMark): Long {
    val micros = (mark - origin).inWholeMicroseconds
    require(micros >= 0) { "The mark is $micros microseconds before the origin" }
    return micros
  }

  // endregion

  // region Topics

  /**
   * Publishes a topic and returns its id, or returns the id of the topic with this [name] if it is
   * already published.
   *
   * Clients subscribed to a matching name are told about the topic right away.
   *
   * @param name The topic name, conventionally starting with `/`. Names starting with `$` are
   *   reserved for the server.
   * @param type The topic's type string, such as `"json"` or `"msgpack"`. Prefer the overload that
   *   takes an [NT4Type] for the types that have one. A type string that isn't one of [NT4Type]'s,
   *   other than `json`, is published as binary, which is how struct topics work.
   * @param retained Whether the topic should stay announced when nothing is publishing to it.
   * @param properties Other topic properties, such as `unit`, announced to clients. Values are
   *   strings, booleans or finite numbers. They are ignored if the topic is already published.
   * @throws IllegalArgumentException if the name is empty or reserved, the name has ever been
   *   published with a different type, or a property value is of another kind. A name keeps its type
   *   for as long as this server object exists, even after [unpublish], because dashboards keep a
   *   field's first type and refuse values of another.
   */
  @JvmOverloads
  fun publish(
    name: String,
    type: String,
    retained: Boolean = false,
    properties: Map<String, Any> = emptyMap(),
  ): Int {
    require(name.isNotEmpty()) { "A topic name can't be empty" }
    require(!name.startsWith("$")) { "Topic names starting with \$ are reserved for the server" }
    synchronized(lock) {
      topicsByName[name]?.let { existing ->
        require(existing.type == type) {
          "Topic '$name' is already published as '${existing.type}', not '$type'"
        }
        return existing.id
      }
      typeEverPublished[name]?.let { earlier ->
        require(earlier == type) {
          "Topic '$name' was published as '$earlier' earlier, and dashboards keep a topic's first " +
            "type, so it can't be '$type'. Use a different name."
        }
      }
      val announcement = announceMessage(name, nextTopicId, type, retained, properties)
      typeEverPublished[name] = type
      val id = nextTopicId++
      val topic = Topic(id, name, type, NT4Type.idOf(type), announcement)
      topicsById[id] = topic
      topicsByName[name] = topic
      for (client in clients) refresh(client, topic)
      return id
    }
  }

  /** Publishes a topic of [type]. See the overload that takes a type string. */
  @JvmOverloads
  fun publish(
    name: String,
    type: NT4Type,
    retained: Boolean = false,
    properties: Map<String, Any> = emptyMap(),
  ): Int = publish(name, type.typeString, retained, properties)

  /**
   * Publishes a topic that holds single values of [schema], and returns its id.
   *
   * Publishes the schema topics for [schema] and every struct type nested in it first, if they
   * haven't been already, so a client can decode the first value.
   *
   * @throws IllegalArgumentException if a different schema with the same type name was already
   *   published
   */
  @JvmOverloads
  fun publishStruct(
    name: String,
    schema: StructSchema<*>,
    retained: Boolean = false,
    properties: Map<String, Any> = emptyMap(),
  ): Int = synchronized(lock) {
    publishSchemas(schema)
    publish(name, schema.typeString, retained, properties)
  }

  /**
   * Publishes a topic that holds arrays of [schema] values, and returns its id. Otherwise the same
   * as [publishStruct].
   */
  @JvmOverloads
  fun publishStructArray(
    name: String,
    schema: StructSchema<*>,
    retained: Boolean = false,
    properties: Map<String, Any> = emptyMap(),
  ): Int = synchronized(lock) {
    publishSchemas(schema)
    publish(name, schema.arrayTypeString, retained, properties)
  }

  /**
   * Removes the topic [id] and tells the clients that were told about it. Does nothing if there is
   * no such topic. The name can be published again, but only with the type it had.
   */
  fun unpublish(id: Int) {
    synchronized(lock) {
      val topic = topicsById.remove(id) ?: return
      topicsByName.remove(topic.name)
      val message = unannounceMessage(topic.name, topic.id)
      for (client in clients) {
        client.receiving.remove(topic.id)
        if (client.announced.remove(topic.id)) enqueue(client, Frame.Text(message))
      }
    }
  }

  /** Publishes the schema topics for [schema], nested types first, skipping those already published. */
  private fun publishSchemas(schema: StructSchema<*>) {
    for (nested in schema.nested) publishSchemas(nested)
    val existing = publishedSchemas[schema.typeName]
    if (existing != null) {
      require(existing == schema.schema) {
        "Struct '${schema.typeName}' was already published with a different schema: " +
          "'$existing', not '${schema.schema}'"
      }
      return
    }
    val id = publish(schema.schemaTopicName, NT4Type.STRUCT_SCHEMA, retained = true)
    // Like WPILib, this is a default value, with timestamp 0, that any later value would replace.
    setRaw(id, schema.schema.toByteArray(Charsets.UTF_8), 0)
    publishedSchemas[schema.typeName] = schema.schema
  }

  // endregion

  // region Values

  /** Sets the value of the `boolean` topic [id]. */
  @JvmOverloads
  fun setBoolean(id: Int, value: Boolean, timestampMicros: Long = now()) =
    setValue(id, NT4Type.BOOLEAN.id, timestampMicros) { it.packBoolean(value) }

  /** Sets the value of the `int` topic [id]. */
  @JvmOverloads
  fun setInteger(id: Int, value: Long, timestampMicros: Long = now()) =
    setValue(id, NT4Type.INTEGER.id, timestampMicros) { it.packLong(value) }

  /** Sets the value of the `float` topic [id]. */
  @JvmOverloads
  fun setFloat(id: Int, value: Float, timestampMicros: Long = now()) =
    setValue(id, NT4Type.FLOAT.id, timestampMicros) { it.packFloat(value) }

  /** Sets the value of the `double` topic [id]. */
  @JvmOverloads
  fun setDouble(id: Int, value: Double, timestampMicros: Long = now()) =
    setValue(id, NT4Type.DOUBLE.id, timestampMicros) { it.packDouble(value) }

  /** Sets the value of the `string` or `json` topic [id]. */
  @JvmOverloads
  fun setString(id: Int, value: String, timestampMicros: Long = now()) =
    setValue(id, NT4Type.STRING.id, timestampMicros) { it.packString(value) }

  /**
   * Sets the value of a binary topic [id], such as a `raw`, `msgpack`, or struct topic. For a
   * struct topic prefer [setStruct], which checks the type and packs the value.
   */
  @JvmOverloads
  fun setRaw(id: Int, value: ByteArray, timestampMicros: Long = now()) =
    setValue(id, NT4Type.RAW.id, timestampMicros) {
      it.packBinaryHeader(value.size)
      it.writePayload(value)
    }

  /** Sets the value of the `boolean[]` topic [id]. */
  @JvmOverloads
  fun setBooleanArray(id: Int, value: BooleanArray, timestampMicros: Long = now()) =
    setValue(id, NT4Type.BOOLEAN_ARRAY.id, timestampMicros) {
      it.packArrayHeader(value.size)
      for (element in value) it.packBoolean(element)
    }

  /** Sets the value of the `int[]` topic [id]. */
  @JvmOverloads
  fun setIntegerArray(id: Int, value: LongArray, timestampMicros: Long = now()) =
    setValue(id, NT4Type.INTEGER_ARRAY.id, timestampMicros) {
      it.packArrayHeader(value.size)
      for (element in value) it.packLong(element)
    }

  /** Sets the value of the `float[]` topic [id]. */
  @JvmOverloads
  fun setFloatArray(id: Int, value: FloatArray, timestampMicros: Long = now()) =
    setValue(id, NT4Type.FLOAT_ARRAY.id, timestampMicros) {
      it.packArrayHeader(value.size)
      for (element in value) it.packFloat(element)
    }

  /** Sets the value of the `double[]` topic [id]. */
  @JvmOverloads
  fun setDoubleArray(id: Int, value: DoubleArray, timestampMicros: Long = now()) =
    setValue(id, NT4Type.DOUBLE_ARRAY.id, timestampMicros) {
      it.packArrayHeader(value.size)
      for (element in value) it.packDouble(element)
    }

  /** Sets the value of the `string[]` topic [id]. */
  @JvmOverloads
  fun setStringArray(id: Int, value: List<String>, timestampMicros: Long = now()) =
    setValue(id, NT4Type.STRING_ARRAY.id, timestampMicros) {
      it.packArrayHeader(value.size)
      for (element in value) it.packString(element)
    }

  /**
   * Sets the value of a topic published with [publishStruct], packing [value] with [schema].
   *
   * @throws IllegalArgumentException if the topic isn't a struct topic for [schema]
   * @throws IllegalStateException if [schema] packs a different number of bytes than it declares
   */
  @JvmOverloads
  fun <T> setStruct(id: Int, schema: StructSchema<T>, value: T, timestampMicros: Long = now()) {
    checkStructTopic(id, schema.typeString)
    val buffer = ByteBuffer.allocate(schema.size).order(ByteOrder.LITTLE_ENDIAN)
    schema.pack(buffer, value)
    checkPacked(schema, buffer, schema.size)
    setRaw(id, buffer.array(), timestampMicros)
  }

  /**
   * Sets the value of a topic published with [publishStructArray], packing [values] back to back
   * with [schema].
   */
  @JvmOverloads
  fun <T> setStructArray(
    id: Int,
    schema: StructSchema<T>,
    values: List<T>,
    timestampMicros: Long = now(),
  ) {
    checkStructTopic(id, schema.arrayTypeString)
    val buffer = ByteBuffer.allocate(schema.size * values.size).order(ByteOrder.LITTLE_ENDIAN)
    for (value in values) schema.pack(buffer, value)
    checkPacked(schema, buffer, schema.size * values.size)
    setRaw(id, buffer.array(), timestampMicros)
  }

  private fun checkStructTopic(id: Int, expectedType: String) {
    val topic = synchronized(lock) { topicsById[id] }
    require(topic != null) { "There is no topic with id $id" }
    require(topic.type == expectedType) {
      "Topic '${topic.name}' has type '${topic.type}', not '$expectedType'"
    }
  }

  private fun checkPacked(schema: StructSchema<*>, buffer: ByteBuffer, expected: Int) {
    check(buffer.position() == expected) {
      "Struct '${schema.typeName}' declares a size of ${schema.size} but packed " +
        "${buffer.position()} bytes for $expected expected"
    }
  }

  /**
   * Encodes a value update for the topic [id], remembers it as the topic's current value, and
   * queues it for every client that should receive it.
   */
  private fun setValue(
    id: Int,
    typeId: Int,
    timestampMicros: Long,
    packValue: (MessageBufferPacker) -> Unit,
  ) {
    require(timestampMicros >= 0) { "A timestamp can't be negative, but was $timestampMicros" }
    synchronized(lock) {
      val topic = topicsById[id]
      require(topic != null) { "There is no topic with id $id" }
      require(topic.typeId == typeId) {
        "Topic '${topic.name}' has type '${topic.type}', which can't take this kind of value"
      }
      writer.clear()
      writer.packArrayHeader(4)
      writer.packLong(topic.id.toLong())
      writer.packLong(timestampMicros)
      writer.packLong(topic.typeId.toLong())
      packValue(writer)
      val frame = writer.toByteArray()
      topic.currentValue = frame
      for (client in clients) {
        if (topic.id in client.receiving) enqueue(client, Frame.Binary(true, frame))
      }
    }
  }

  // endregion

  // region Clients

  private suspend fun DefaultWebSocketServerSession.serve(protocol: Protocol) {
    if (protocol == Protocol.V4_1) {
      // The 4.1 protocol asks both sides to ping. Version 4.0 clients mustn't be sent pings.
      pingIntervalMillis = PING_INTERVAL_MILLIS
      timeoutMillis = PING_TIMEOUT_MILLIS
    }
    val client = Client(Channel(CLIENT_QUEUE_CAPACITY))
    if (protocol != Protocol.RTT) synchronized(lock) { clients.add(client) }

    val sender = launch {
      try {
        for (frame in client.queue) send(frame)
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        // The connection went away while sending, and the receive loop below is ending too.
        return@launch
      }
      if (client.overflowed) {
        close(CloseReason(CloseReason.Codes.TRY_AGAIN_LATER, "Client can't keep up"))
      }
    }
    try {
      for (frame in incoming) {
        when (frame) {
          is Frame.Text -> if (protocol != Protocol.RTT) handleText(client, frame.readText())
          is Frame.Binary -> handleBinary(client, frame.readBytes())
          else -> {}
        }
      }
    } finally {
      synchronized(lock) { clients.remove(client) }
      client.queue.close()
      sender.cancel()
    }
  }

  private fun handleText(client: Client, text: String) {
    val messages =
      try {
        Json.parseToJsonElement(text) as? JsonArray
      } catch (e: IllegalArgumentException) {
        null
      } ?: return
    for (message in messages) {
      val obj = message as? JsonObject ?: continue
      val method = (obj["method"] as? JsonPrimitive)?.takeIf { it.isString }?.content ?: continue
      val params = obj["params"] as? JsonObject ?: continue
      when (method) {
        "subscribe" -> subscribe(client, params)
        "unsubscribe" -> unsubscribe(client, params)
      }
    }
  }

  private fun subscribe(client: Client, params: JsonObject) {
    val topics =
      (params["topics"] as? JsonArray)?.map { name ->
        (name as? JsonPrimitive)?.takeIf { it.isString }?.content ?: return
      } ?: return
    val subscriptionId = (params["subuid"] as? JsonPrimitive)?.intOrNull ?: return
    val options = params["options"] as? JsonObject
    fun option(key: String) = (options?.get(key) as? JsonPrimitive)?.booleanOrNull ?: false

    synchronized(lock) {
      client.subscriptions[subscriptionId] =
        Subscription(topics, prefix = option("prefix"), topicsOnly = option("topicsonly"))
      for (topic in topicsById.values) refresh(client, topic)
    }
  }

  private fun unsubscribe(client: Client, params: JsonObject) {
    val subscriptionId = (params["subuid"] as? JsonPrimitive)?.intOrNull ?: return
    synchronized(lock) {
      if (client.subscriptions.remove(subscriptionId) == null) return
      for (topic in topicsById.values) refresh(client, topic)
    }
  }

  /** Answers the clock-synchronization messages in a binary frame, and ignores everything else. */
  private fun handleBinary(client: Client, bytes: ByteArray) {
    try {
      val unpacker = MessagePack.newDefaultUnpacker(bytes)
      while (unpacker.hasNext()) {
        if (unpacker.nextFormat.valueType != ValueType.ARRAY) {
          unpacker.skipValue()
          continue
        }
        val size = unpacker.unpackArrayHeader()
        if (size != 4) {
          repeat(size) { unpacker.skipValue() }
          continue
        }
        val id = unpacker.unpackLongOrSkip()
        unpacker.skipValue() // The timestamp, which the client sets to 0
        val typeId = unpacker.unpackLongOrSkip()
        val clientTime: Any? =
          when (unpacker.nextFormat) {
            MessageFormat.FLOAT32 -> unpacker.unpackFloat()
            MessageFormat.FLOAT64 -> unpacker.unpackDouble()
            else -> unpacker.unpackLongOrSkip()
          }
        if (id != -1L || typeId == null || clientTime == null) continue

        val reply = MessagePack.newDefaultBufferPacker()
        reply.packArrayHeader(4)
        reply.packLong(-1)
        reply.packLong(now())
        reply.packLong(typeId)
        when (clientTime) {
          is Long -> reply.packLong(clientTime)
          is Float -> reply.packFloat(clientTime)
          is Double -> reply.packDouble(clientTime)
          else -> continue
        }
        synchronized(lock) { enqueue(client, Frame.Binary(true, reply.toByteArray())) }
      }
    } catch (e: MessagePackException) {
      // A malformed frame is ignored, along with whatever was left of it.
    } catch (e: java.io.IOException) {
      // The same goes for a frame that ends early.
    }
  }

  private fun MessageUnpacker.unpackLongOrSkip(): Long? = if (nextFormat.valueType == ValueType.INTEGER) {
    unpackLong()
  } else {
    skipValue()
    null
  }

  /**
   * Brings [client] up to date about [topic]: announces it if a subscription matches and it hasn't
   * been announced, and starts or stops sending its values. Call with [lock] held.
   */
  private fun refresh(client: Client, topic: Topic) {
    var matches = false
    var wantsValues = false
    for (subscription in client.subscriptions.values) {
      if (subscription.matches(topic.name)) {
        matches = true
        if (!subscription.topicsOnly) wantsValues = true
      }
    }
    if (matches && client.announced.add(topic.id)) enqueue(client, Frame.Text(topic.announcement))
    if (wantsValues) {
      if (client.receiving.add(topic.id)) {
        topic.currentValue?.let { enqueue(client, Frame.Binary(true, it)) }
      }
    } else {
      client.receiving.remove(topic.id)
    }
  }

  /**
   * Queues [frame] for [client] without blocking. If the queue is full the client is too slow, so
   * it is disconnected to resync from scratch when it reconnects. Call with [lock] held.
   */
  private fun enqueue(client: Client, frame: Frame) {
    val result = client.queue.trySend(frame)
    if (result.isFailure && !result.isClosed) {
      client.overflowed = true
      client.queue.close()
    }
  }

  // endregion

  private enum class Protocol {
    V4_1,
    V4_0,
    RTT,
  }

  private class Topic(
    val id: Int,
    val name: String,
    val type: String,
    val typeId: Int,
    /** The text frame that announces the topic. */
    val announcement: String,
  ) {
    /** The binary frame for the latest value, which is what a new subscriber is sent. */
    var currentValue: ByteArray? = null
  }

  private class Subscription(val topics: List<String>, val prefix: Boolean, val topicsOnly: Boolean) {
    fun matches(name: String): Boolean = topics.any { if (prefix) name.startsWith(it) else name == it }
  }

  private class Client(val queue: Channel<Frame>) {
    /** Subscriptions by the client's own ids for them. */
    val subscriptions = HashMap<Int, Subscription>()

    /** The ids of the topics the client has been told about. */
    val announced = HashSet<Int>()

    /** The ids of the topics whose values the client is sent. */
    val receiving = HashSet<Int>()

    /** Set when the queue filled up, and the client is being disconnected for it. */
    @Volatile var overflowed = false
  }

  companion object {
    /** The standard unsecured NetworkTables 4 port. */
    const val DEFAULT_PORT = 5810

    /** The WebSocket subprotocol of NetworkTables 4.1, which clients should prefer. */
    const val PROTOCOL_V4_1 = "v4.1.networktables.first.wpi.edu"

    /** The WebSocket subprotocol of NetworkTables 4.0. */
    const val PROTOCOL_V4_0 = "networktables.first.wpi.edu"

    /** The subprotocol for a connection that only measures round-trip time. */
    const val PROTOCOL_RTT = "rtt.networktables.first.wpi.edu"

    /** The number of frames queued for one client before it is dropped for being too slow. */
    private const val CLIENT_QUEUE_CAPACITY = 16_384

    private const val PING_INTERVAL_MILLIS = 1_000L
    private const val PING_TIMEOUT_MILLIS = 5_000L

    private fun announceMessage(
      name: String,
      id: Int,
      type: String,
      retained: Boolean,
      properties: Map<String, Any>,
    ): String {
      val json = properties.toJsonObject()
      return buildJsonArray {
        add(
          buildJsonObject {
            put("method", "announce")
            put(
              "params",
              buildJsonObject {
                put("name", name)
                put("id", id)
                put("type", type)
                put(
                  "properties",
                  buildJsonObject {
                    for ((key, value) in json) put(key, value)
                    if (retained) put("retained", true)
                  },
                )
              },
            )
          },
        )
      }
        .toString()
    }

    private fun unannounceMessage(name: String, id: Int): String = buildJsonArray {
      add(
        buildJsonObject {
          put("method", "unannounce")
          put(
            "params",
            buildJsonObject {
              put("name", name)
              put("id", id)
            },
          )
        },
      )
    }
      .toString()
  }
}
