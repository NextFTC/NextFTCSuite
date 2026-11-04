/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.telemetry.ftc

import dev.nextftc.telemetry.TelemetryRegistry
import dev.nextftc.telemetry.withUnit
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.firstinspires.ftc.robotcore.external.Telemetry as SdkTelemetry

// The backend calls the SDK's Object overloads, so the tests have to name them too
private fun text(value: String): Any = value

class DriverStationBackendTest :
  FunSpec({
    fun fixture(): Triple<TelemetryRegistry, SdkTelemetry, SdkTelemetry.Item> {
      val item = mockk<SdkTelemetry.Item>(relaxed = true)
      val sdk = mockk<SdkTelemetry>(relaxed = true)
      every { sdk.addData(any<String>(), any<Any>()) } returns item
      return Triple(TelemetryRegistry(), sdk, item)
    }

    test("a path gets one retained line, named without the leading slash") {
      val (registry, sdk, item) = fixture()
      registry.register(DriverStationBackend(sdk))

      registry.table("Drive").log("left", 1.5)
      registry.table("Drive").log("left", 2.5)

      verify(exactly = 1) { sdk.addData("Drive/left", text("")) }
      verify { item.setRetained(true) }
      verify { item.setValue(text("1.5")) }
      verify { item.setValue(text("2.5")) }
    }

    test("a unit is shown after the value") {
      val (registry, sdk, item) = fixture()
      registry.register(DriverStationBackend(sdk))

      registry.table("Drive").doubleTopic("speed").withUnit("m/s").set(3.0)

      verify { item.setValue(text("3.0 m/s")) }
    }

    test("arrays are shown by their contents") {
      val (registry, sdk, item) = fixture()
      registry.register(DriverStationBackend(sdk))

      registry.root.log("wheels", doubleArrayOf(1.0, 2.0))

      verify { item.setValue(text("[1.0, 2.0]")) }
    }

    test("a value with a percent sign is not treated as a format string") {
      val (registry, sdk, item) = fixture()
      registry.register(DriverStationBackend(sdk))

      registry.root.log("battery", "100%")

      verify { item.setValue(text("100%")) }
      verify(exactly = 0) { item.setValue(any<String>(), *anyVararg()) }
    }

    test("logging doesn't update the SDK telemetry, update does") {
      val (registry, sdk, _) = fixture()
      val backend = DriverStationBackend(sdk)
      registry.register(backend)

      registry.root.log("a", 1)
      verify(exactly = 0) { sdk.update() }

      backend.update()
      verify(exactly = 1) { sdk.update() }
    }

    test("closing removes the lines and stops showing values") {
      val (registry, sdk, item) = fixture()
      val backend = DriverStationBackend(sdk)
      registry.register(backend)
      registry.root.log("a", 1)

      backend.close()
      registry.root.log("a", 2)

      verify { sdk.removeItem(item) }
      verify(exactly = 0) { item.setValue(text("2")) }
    }
  })
