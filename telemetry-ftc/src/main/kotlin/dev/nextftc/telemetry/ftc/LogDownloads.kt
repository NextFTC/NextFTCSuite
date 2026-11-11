/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.telemetry.ftc

import io.ktor.http.ContentDisposition
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.withCharset
import io.ktor.server.http.content.LocalFileContent
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondText
import io.ktor.server.routing.Routing
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import java.io.DataInputStream
import java.io.File
import java.io.IOException
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Routes that serve the WPILOGs [FtcTelemetry] wrote over HTTP, on the NetworkTables server's port:
 *
 * - `/download` is a page that lists them, newest first, with a link for each. It is the static
 *   files in [PAGE_RESOURCES], and a script in it reads `/logs`.
 * - `/logs` is the list as JSON.
 * - `/logs/<name>` downloads one.
 *
 * Only `.wpilog` files directly inside the directory can be reached, and a log that is still being
 * written is listed but can't be downloaded, since the file would be cut off.
 *
 * @param directory Where the logs are, read on every request so a change to the setting applies
 * @param activeFile The log being written right now, if any
 */
internal class LogDownloads(private val directory: () -> File, private val activeFile: () -> File?) {
  internal class Entry(
    val file: File,
    /** The OpMode's name, from the file's header or else its name. */
    val opMode: String,
    /** When the OpMode was initialized, in the Robot Controller's time zone, or null if the name has no stamp. */
    val started: Date?,
    val sizeBytes: Long,
    val modifiedMillis: Long,
    val recording: Boolean,
  ) {
    val name: String get() = file.name
  }

  fun install(routing: Routing) = with(routing) {
    for (path in listOf(PAGE_PATH, "$PAGE_PATH/")) {
      get(path) { respondPageFile("index.html") }
    }

    get("$PAGE_PATH/{file}") { respondPageFile(call.parameters["file"]) }

    get("/logs") {
      call.respondText(withContext(Dispatchers.IO) { json(entries()) }, ContentType.Application.Json)
    }

    get("/logs/{name}") {
      val name = call.parameters["name"]
      val entry = withContext(Dispatchers.IO) { entries().firstOrNull { it.name == name } }
      when {
        entry == null -> call.respondText("Not found", status = HttpStatusCode.NotFound)
        entry.recording -> call.respondText(
          "That log is still being written. Stop the OpMode first.",
          status = HttpStatusCode.Conflict,
        )
        else -> {
          call.response.header(
            HttpHeaders.ContentDisposition,
            ContentDisposition.Attachment.withParameter(
              ContentDisposition.Parameters.FileName,
              entry.name,
            ).toString(),
          )
          call.respond(LocalFileContent(entry.file, ContentType.Application.OctetStream))
        }
      }
    }
  }

  private suspend fun RoutingContext.respondPageFile(name: String?) {
    val type = PAGE_FILES[name]
    val bytes = if (type == null) {
      null
    } else {
      withContext(Dispatchers.IO) {
        LogDownloads::class.java.classLoader?.getResourceAsStream("$PAGE_RESOURCES/$name")?.use {
          it.readBytes()
        }
      }
    }
    if (type == null || bytes == null) {
      call.respondText("Not found", status = HttpStatusCode.NotFound)
      return
    }
    call.response.header(HttpHeaders.CacheControl, "no-cache")
    call.respondBytes(bytes, type)
  }

  /** Every log in the directory, newest first. */
  internal fun entries(): List<Entry> {
    val active = activeFile()?.let { runCatching { it.canonicalFile }.getOrNull() }
    val files = directory().listFiles { f -> f.isFile && f.name.endsWith(EXTENSION) } ?: return emptyList()
    return files
      .map { file ->
        val parsed = NAME.matchEntire(file.name)
        val started = parsed?.let { parseStamp(it.groupValues[1]) }
        Entry(
          file = file,
          opMode =
          readOpMode(file) ?: parsed?.groupValues?.get(2)?.replace('_', ' ')
            ?: file.name.removeSuffix(EXTENSION),
          started = started,
          sizeBytes = file.length(),
          modifiedMillis = file.lastModified(),
          recording = active != null && runCatching { file.canonicalFile }.getOrNull() == active,
        )
      }
      .sortedWith(
        compareByDescending<Entry> {
          it.started?.time ?: it.modifiedMillis
        }.thenByDescending { it.name },
      )
  }

  private fun json(entries: List<Entry>): String {
    val shown = SimpleDateFormat("EEE MMM d, yyyy  h:mm:ss a", Locale.US)
    return buildJsonArray {
      for (entry in entries) {
        addJsonObject {
          put("name", entry.name)
          put("opMode", entry.opMode)
          put("started", entry.started?.time)
          put("startedText", shown.format(entry.started ?: Date(entry.modifiedMillis)))
          put("sizeBytes", entry.sizeBytes)
          put("modified", entry.modifiedMillis)
          put("recording", entry.recording)
        }
      }
    }
      .toString()
  }

  private companion object {
    const val EXTENSION = ".wpilog"

    /** Where the page is served. */
    const val PAGE_PATH = "/download"

    /**
     * Where the page is located in the resources directory.
     */
    const val PAGE_RESOURCES = "dev/nextftc/telemetry/ftc/download"

    /** The files of the page, with their types. */
    val PAGE_FILES = mapOf(
      "index.html" to ContentType.Text.Html.withCharset(Charsets.UTF_8),
      "logs.css" to ContentType.Text.CSS.withCharset(Charsets.UTF_8),
      "logs.js" to ContentType.Application.JavaScript.withCharset(Charsets.UTF_8),
    )

    /** The name [FtcTelemetry] gives a log: the stamp, then the sanitized OpMode name. */
    val NAME = Regex("""(\d{4}_\d{2}_\d{2}__\d{2}_\d{2}_\d{2})__(.*)\.wpilog""")

    fun parseStamp(stamp: String): Date? = try {
      SimpleDateFormat("yyyy_MM_dd__HH_mm_ss", Locale.US).apply { isLenient = false }.parse(stamp)
    } catch (e: ParseException) {
      null
    }

    /** Reads the OpMode's real name from the header [FtcTelemetry] wrote, since the file name is sanitized. */
    fun readOpMode(file: File): String? = try {
      DataInputStream(file.inputStream().buffered()).use { input ->
        // "WPILOG", a 2-byte version, a 4-byte little-endian length, then the extra header text
        val fixed = ByteArray(12)
        input.readFully(fixed)
        if (String(fixed, 0, 6, Charsets.US_ASCII) != "WPILOG") return null
        val length = (fixed[8].toInt() and 0xFF) or ((fixed[9].toInt() and 0xFF) shl 8) or
          ((fixed[10].toInt() and 0xFF) shl 16) or ((fixed[11].toInt() and 0xFF) shl 24)
        if (length !in 1..4096) return null
        val text = ByteArray(length).also { input.readFully(it) }.toString(Charsets.UTF_8)
        val header = try {
          Json.parseToJsonElement(text) as? JsonObject
        } catch (e: SerializationException) {
          null
        }
        (header?.get("opMode") as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }
      }
    } catch (e: IOException) {
      null
    }
  }
}
