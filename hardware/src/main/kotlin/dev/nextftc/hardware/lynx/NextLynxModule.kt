package dev.nextftc.hardware.lynx

import android.R
import com.qualcomm.hardware.lynx.LynxDcMotorController
import com.qualcomm.hardware.lynx.LynxDigitalChannelController
import com.qualcomm.hardware.lynx.LynxI2cDeviceSynch
import com.qualcomm.hardware.lynx.LynxI2cDeviceSynchV2
import com.qualcomm.hardware.lynx.LynxModule
import com.qualcomm.hardware.lynx.LynxServoController
import dev.nextftc.hardware.RobotController
import dev.nextftc.hardware.util.LazyHardware
import dev.nextftc.units.celsius
import dev.nextftc.units.measuretypes.Temperature
import dev.nextftc.units.measuretypes.Voltage
import dev.nextftc.units.volts
import org.firstinspires.ftc.robotcore.external.navigation.TempUnit
import org.firstinspires.ftc.robotcore.external.navigation.VoltageUnit

/**
 * Represents a Lynx module. A module can be a control hub, expansion hub, or servo hub.
 *
 * Users should not instantiate this class directly. Instead, use [RobotController.controlHub], [RobotController.expansionHub], or [RobotController.servoHubs] to access the available modules.
 */
class NextLynxModule internal constructor(initializer: () -> LynxModule, @JvmField val type: Type) {
  enum class Type {
    CONTROL_HUB,
    EXPANSION_HUB,
    SERVO_HUB,
  }

  private val module by LazyHardware(initializer)

  private val i2cControllers = Array(4) { bus ->
    LazyHardware { LynxI2cDeviceSynchV2(RobotController.appContext, module, bus) }
  }

  /** Current module temperature. */
  val temperature: Temperature
    get() = module.getTemperature(TempUnit.CELSIUS).celsius

  /** Current module input voltage. */
  val inputVoltage: Voltage
    get() = module.getInputVoltage(VoltageUnit.VOLTS).volts

  /** Current module auxiliary voltage. */
  val auxiliaryVoltage: Voltage
    get() = module.getAuxiliaryVoltage(VoltageUnit.VOLTS).volts

  /** Creates or gets a [LynxDcMotorController] bound to this module. */
  val motorController: LynxDcMotorController by LazyHardware {
    LynxDcMotorController(RobotController.appContext, module)
  }

  /** Creates or gets a [LynxServoController] bound to this module. */
  val servoController: LynxServoController by LazyHardware {
    LynxServoController(RobotController.appContext, module)
  }

  /** Creates or gets a [LynxDigitalController] bound to this module. */
  fun i2cController(bus: Int): LynxI2cDeviceSynch {
    require(bus in 0..3) { "I2C bus must be in range of 0 - 3, got $bus" }
    return i2cControllers[bus].get()
  }

  /** Creates or gets a [LynxDigitalController] bound to this module. */
  val digitalController: LynxDigitalChannelController by LazyHardware {
    LynxDigitalChannelController(RobotController.appContext, module)
  }
}
