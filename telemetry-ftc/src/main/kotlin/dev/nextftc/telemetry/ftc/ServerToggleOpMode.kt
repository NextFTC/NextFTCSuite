/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.telemetry.ftc

import com.qualcomm.robotcore.eventloop.opmode.OpMode
import dev.frozenmilk.sinister.sdk.apphooks.SinisterOpModeRegistrar
import dev.frozenmilk.sinister.sdk.opmodes.OpModeScanner
import org.firstinspires.ftc.robotcore.internal.opmode.OpModeMeta
import java.util.function.Supplier

/**
 * An OpMode that turns the NetworkTables server on or off with [FtcTelemetry.setServerEnabled], so a
 * team can disable it for competition and enable it again afterward from the Driver Station. The
 * choice is saved on the Robot Controller.
 */
internal class ServerToggleOpMode(private val enable: Boolean) : OpMode() {
  override fun init() {
    FtcTelemetry.setServerEnabled(enable)
    if (enable) {
      telemetry.addLine("NextFTC telemetry server: ENABLED")
      telemetry.addLine("It starts when you initialize your next OpMode.")
    } else {
      telemetry.addLine("NextFTC telemetry server: DISABLED")
      telemetry.addLine("It is stopped, and stays off until you enable it again.")
    }
    telemetry.addLine("This is saved, so it holds after the app restarts.")
    telemetry.update()
  }

  override fun loop() {}
}

/** Registers the OpModes that switch the server, using Sloth, so no NextFTC OpMode base is needed. */
internal object ServerToggleOpModes : SinisterOpModeRegistrar {
  private const val DISABLE_NAME = "Disable NextFTC Telemetry Server"
  private const val ENABLE_NAME = "Enable NextFTC Telemetry Server"

  override fun registerOpModes(registrationHelper: OpModeScanner.RegistrationHelper) {
    registrationHelper.register(
      meta(DISABLE_NAME, "Stops the NetworkTables server, e.g. for competition. Saved across restarts."),
      Supplier { ServerToggleOpMode(enable = false) },
    )
    registrationHelper.register(
      meta(ENABLE_NAME, "Lets the NetworkTables server run again. Saved across restarts."),
      Supplier { ServerToggleOpMode(enable = true) },
    )
  }

  private fun meta(name: String, description: String): OpModeMeta = OpModeMeta.Builder()
    .setFlavor(OpModeMeta.Flavor.UTILITY)
    .setName(name)
    .setDescription(description)
    .setSource(OpModeMeta.Source.ANDROID_STUDIO)
    .build()
}
