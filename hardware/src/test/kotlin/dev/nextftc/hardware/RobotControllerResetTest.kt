package dev.nextftc.hardware

import dev.nextftc.hardware.util.LazyHardware
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class RobotControllerResetTest : FunSpec({
  test("resetHardwareState clears cached lazy hardware values") {
    var calls = 0
    class Holder {
      val value by LazyHardware { ++calls }
    }

    val holder = Holder()
    holder.value shouldBe 1

    RobotController.resetHardwareState()

    holder.value shouldBe 2
    calls shouldBe 2
  }
})
