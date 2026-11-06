/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.telemetry.ftc

import dev.nextftc.telemetry.nt.NT4Server
import dev.nextftc.telemetry.wpilog.WPILogWriter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.text.SimpleDateFormat
import java.util.Locale

private class HttpResult(
  val code: Int,
  val contentType: String?,
  val headers: Map<String, String?>,
  val bytes: ByteArray,
) {
  val text: String get() = String(bytes, Charsets.UTF_8)
}

/** Requests to a running [NT4Server], over real HTTP. */
private class Client(private val server: NT4Server) {
  fun get(path: String): HttpResult {
    val connection = URL("http://127.0.0.1:${server.boundPort}$path").openConnection() as HttpURLConnection
    val code = connection.responseCode
    val body =
      (if (code < 400) connection.inputStream else connection.errorStream)?.readBytes() ?: ByteArray(0)
    val headers = listOf("Cache-Control", "Content-Disposition").associateWith {
      connection.getHeaderField(it)
    }
    return HttpResult(code, connection.contentType, headers, body)
  }

  fun logs(): List<JsonObject> = Json.parseToJsonElement(get("/logs").text).jsonArray.map { it.jsonObject }
}

class LogDownloadsTest :
  FunSpec({
    fun withDirectory(block: (File) -> Unit) {
      val dir = Files.createTempDirectory("logs").toFile()
      try {
        block(dir)
      } finally {
        dir.deleteRecursively()
      }
    }

    /** Serves [dir] the way FtcTelemetry does, with [active] as the log being written. */
    fun withServer(dir: File, active: File? = null, block: (Client) -> Unit) {
      val logDownloads = LogDownloads({ dir }, { active })
      val server = NT4Server(port = 0, host = "127.0.0.1", routes = { logDownloads.install(this) })
      server.start()
      try {
        block(Client(server))
      } finally {
        server.stop()
      }
    }

    /** A real log, with the header [FtcTelemetry] writes. */
    fun writeLog(dir: File, name: String, opMode: String): File {
      val file = File(dir, name)
      val header = buildJsonObject {
        put("source", "NextFTC")
        put("opMode", opMode)
      }
      WPILogWriter(FileOutputStream(file), extraHeader = header.toString()).close()
      return file
    }

    test("lists logs newest first with the OpMode name from the header") {
      withDirectory { dir ->
        writeLog(dir, "2026_11_05__10_00_00__Old_Auto.wpilog", "Old Auto")
        writeLog(dir, "2026_11_06__09_30_15__Red_Teleop.wpilog", "Red Teleop")
        File(dir, "notes.txt").writeText("not a log")

        withServer(dir) { client ->
          val response = client.get("/logs")

          response.code shouldBe 200
          response.contentType shouldContain "application/json"
          response.text shouldNotContain "notes.txt"
          client.logs().map { it["opMode"]!!.jsonPrimitive.content } shouldBe
            listOf("Red Teleop", "Old Auto")
        }
      }
    }

    test("falls back to the file name when the header has no OpMode") {
      withDirectory { dir ->
        File(dir, "2026_11_06__09_30_15__Some_OpMode.wpilog").writeText("garbage")

        val entries = LogDownloads({ dir }, { null }).entries()

        entries.single().opMode shouldBe "Some OpMode"
        (entries.single().started != null) shouldBe true
      }
    }

    test("the JSON has what the page shows, with the OpMode name left as it is") {
      withDirectory { dir ->
        writeLog(dir, "2026_11_06__09_30_15__x.wpilog", "<b>Evil & co</b>")

        withServer(dir) { client ->
          val log = client.logs().single()

          // The page sets it as text, never as HTML, so it isn't escaped here
          log["opMode"]?.jsonPrimitive?.content shouldBe "<b>Evil & co</b>"
          log["name"]?.jsonPrimitive?.content shouldBe "2026_11_06__09_30_15__x.wpilog"
          log["startedText"]?.jsonPrimitive?.content shouldBe "Fri Nov 6, 2026  9:30:15 AM"
          log["started"]?.jsonPrimitive?.long shouldBe
            SimpleDateFormat("yyyy_MM_dd__HH_mm_ss", Locale.US).parse("2026_11_06__09_30_15")!!.time
          log["recording"]?.jsonPrimitive?.boolean shouldBe false
          (log["sizeBytes"]?.jsonPrimitive?.long ?: 0) shouldBeGreaterThan 0L
          log.keys shouldBe
            setOf("name", "opMode", "started", "startedText", "sizeBytes", "modified", "recording")
        }
      }
    }

    test("OpMode names with quotes and backslashes survive the log header and the JSON") {
      withDirectory { dir ->
        val tricky = "He said \"hi\" \\ back\nnew line"
        writeLog(dir, "2026_11_06__09_30_15__x.wpilog", tricky)

        withServer(dir) { client ->
          client.logs().single()["opMode"]?.jsonPrimitive?.content shouldBe tricky
        }
      }
    }

    test("a log without a stamp in its name is listed with a null start") {
      withDirectory { dir ->
        File(dir, "custom.wpilog").writeText("garbage")

        withServer(dir) { client ->
          val log = client.logs().single()

          log["started"] shouldBe JsonNull
          log["opMode"]?.jsonPrimitive?.content shouldBe "custom"
        }
      }
    }

    test("an empty or missing directory is an empty list") {
      val missing = File(Files.createTempDirectory("logs").toFile(), "nope")

      withServer(missing) { client -> client.get("/logs").text shouldBe "[]" }
    }

    test("downloads a log by name") {
      withDirectory { dir ->
        val file = writeLog(dir, "2026_11_06__09_30_15__x.wpilog", "x")

        withServer(dir) { client ->
          val response = client.get("/logs/${file.name}")

          response.code shouldBe 200
          response.bytes.toList() shouldBe file.readBytes().toList()
          response.headers["Content-Disposition"] shouldContain "attachment"
          response.headers["Content-Disposition"] shouldContain file.name
        }
      }
    }

    test("won't serve anything outside the listed logs") {
      withDirectory { dir ->
        val outside = File(dir.parentFile, "secret-${dir.name}.wpilog").apply { writeText("secret") }
        try {
          writeLog(dir, "2026_11_06__09_30_15__x.wpilog", "x")
          File(dir, "other.txt").writeText("text")

          withServer(dir) { client ->
            for (path in listOf(
              "/logs/../${outside.name}",
              "/logs/..%2F${outside.name}",
              "/logs/${outside.absolutePath}",
              "/logs/other.txt",
              "/logs/missing.wpilog",
            )) {
              client.get(path).code shouldBe 404
            }
          }
        } finally {
          outside.delete()
        }
      }
    }

    test("a log still being written is listed but refused") {
      withDirectory { dir ->
        val active = writeLog(dir, "2026_11_06__09_30_15__x.wpilog", "x")

        withServer(dir, active) { client ->
          client.get("/logs/${active.name}").code shouldBe 409
          client.logs().single()["recording"]?.jsonPrimitive?.boolean shouldBe true
        }
      }
    }

    test("serves the page and its files as they are") {
      withDirectory { dir ->
        withServer(dir) { client ->
          for (path in listOf("/download", "/download/", "/download/index.html")) {
            val page = client.get(path)
            page.code shouldBe 200
            page.contentType shouldContain "text/html"
            page.text shouldContain "href=\"/download/logs.css\""
            page.text shouldContain "src=\"/download/logs.js\""
            page.text shouldNotContain "<style>"
          }

          val css = client.get("/download/logs.css")
          css.code shouldBe 200
          css.contentType shouldContain "text/css"
          css.text shouldContain "--accent"
          css.headers["Cache-Control"] shouldContain "no-cache"

          val js = client.get("/download/logs.js")
          js.code shouldBe 200
          js.contentType shouldContain "javascript"
          js.text shouldContain "fetch(\"/logs\""

          client.get("/download/nothing.js").code shouldBe 404
          // A path can't climb out of the folder to other classpath files
          for (escape in listOf("%2e%2e/%2e%2e/LogDownloads.class", "..%2f..%2fLogDownloads.class")) {
            client.get("/download/$escape").code shouldNotBe 200
          }
        }
      }
    }
  })
