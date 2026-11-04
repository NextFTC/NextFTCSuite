/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.telemetry.backend

import dev.nextftc.telemetry.TelemetryRegistry
import dev.nextftc.telemetry.nt.NT4Backend
import dev.nextftc.telemetry.nt.NT4Server
import dev.nextftc.telemetry.nt.connect
import dev.nextftc.telemetry.nt.nextAnnounce
import dev.nextftc.telemetry.nt.nextValue
import dev.nextftc.telemetry.nt.sendText
import dev.nextftc.telemetry.nt.subscribeMessage
import dev.nextftc.telemetry.wpilog.ParsedLog
import dev.nextftc.telemetry.wpilog.WPILogBackend
import dev.nextftc.telemetry.wpilog.WPILogWriter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.WebSockets
import java.io.ByteArrayOutputStream
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TestTimeSource

class BackendsTogetherTest :
  FunSpec({
    test("a log file and a live server stamp every value with the same time") {
      val clock = TestTimeSource()
      val registry = TelemetryRegistry(clock)
      val bytes = ByteArrayOutputStream()
      val server = NT4Server(port = 0, host = "127.0.0.1", timeSource = clock, origin = registry.origin)
      server.start()
      val http = HttpClient(CIO) { install(WebSockets) }
      try {
        registry.register(
          WPILogBackend(WPILogWriter(bytes, timeSource = clock, origin = registry.origin)),
        )
        registry.register(NT4Backend(server))

        val handle = registry.table("Drive").doubleTopic("velocity")
        clock += 12.milliseconds
        handle.set(3.5)

        http.connect(server) {
          sendText(subscribeMessage(1, "/Drive/velocity"))
          nextAnnounce()
          val onTheWire = nextValue()

          registry.reset()
          val inTheLog = ParsedLog(bytes.toByteArray()).records.single { !it.isControl }
          onTheWire[1] shouldBe 12_000L
          inTheLog.timestamp shouldBe 12_000L
          onTheWire[3] shouldBe inTheLog.double
        }
      } finally {
        http.close()
        registry.reset()
        server.stop()
      }
    }

    test("prefix routing sends some paths live and everything to the file") {
      val clock = TestTimeSource()
      val registry = TelemetryRegistry(clock)
      val bytes = ByteArrayOutputStream()
      val server = NT4Server(port = 0, host = "127.0.0.1", timeSource = clock, origin = registry.origin)
      server.start()
      val http = HttpClient(CIO) { install(WebSockets) }
      try {
        registry.register(
          WPILogBackend(WPILogWriter(bytes, timeSource = clock, origin = registry.origin)),
        )
        registry.register(NT4Backend(server), "/Drive")

        registry.table("Drive").log("live", 1.0)
        registry.table("Debug").log("fileOnly", 2.0)

        http.connect(server) {
          sendText(subscribeMessage(1, "/", prefix = true))
          nextAnnounce()["name"].toString() shouldBe "\"/Drive/live\""
          nextValue()[3] shouldBe 1.0

          registry.reset()
          val names =
            ParsedLog(bytes.toByteArray())
              .records
              .filter { it.isControl }
              .map { it.startStrings[0] }
          names shouldBe listOf("/Drive/live", "/Debug/fileOnly")
        }
      } finally {
        http.close()
        registry.reset()
        server.stop()
      }
    }
  })
