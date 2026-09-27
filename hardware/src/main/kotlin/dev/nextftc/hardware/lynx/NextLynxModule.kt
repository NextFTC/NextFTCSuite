package dev.nextftc.hardware.lynx

import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver
import com.qualcomm.hardware.lynx.LynxAnalogInputController
import com.qualcomm.hardware.lynx.LynxDcMotorController
import com.qualcomm.hardware.lynx.LynxDigitalChannelController
import com.qualcomm.hardware.lynx.LynxI2cColorRangeSensor
import com.qualcomm.hardware.lynx.LynxI2cDeviceSynch
import com.qualcomm.hardware.lynx.LynxI2cDeviceSynchV2
import com.qualcomm.hardware.lynx.LynxModule
import com.qualcomm.hardware.lynx.LynxServoController
import com.qualcomm.robotcore.hardware.AnalogInput
import com.qualcomm.robotcore.hardware.CRServoImplEx
import com.qualcomm.robotcore.hardware.DcMotorImplEx
import com.qualcomm.robotcore.hardware.DigitalChannel
import com.qualcomm.robotcore.hardware.DigitalChannelImpl
import com.qualcomm.robotcore.hardware.ServoImplEx
import com.qualcomm.robotcore.hardware.configuration.typecontainers.ServoConfigurationType
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

  enum class PortKind {
    MOTOR,
    SERVO,
    I2C,
    DIGITAL,
    ANALOG,
  }

  /** Tracks used ports. Resets each OpMode. */
  private val usedPorts by LazyHardware { HashSet<Pair<PortKind, Int>>() }

  /** Throws if this port is already used on this hub. */
  private fun claim(kind: PortKind, port: Int) {
    check(usedPorts.add(kind to port)) {
      "Port conflict on $type: ${kind.name.lowercase()} port $port is used by more than one device"
    }
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

  /** Creates or gets a [LynxI2cDeviceSynch] bound to this module. */
  fun i2cController(bus: Int): LynxI2cDeviceSynch {
    require(bus in 0..3) { "I2C bus must be in range of 0 - 3, got $bus" }
    return i2cControllers[bus].getValue(this, ::i2cControllers)
  }

  /** Creates or gets a [LynxDigitalChannelController] bound to this module. */
  val digitalController: LynxDigitalChannelController by LazyHardware {
    LynxDigitalChannelController(RobotController.appContext, module)
  }

  /** Creates or gets a [LynxAnalogInputController] bound to this module. */
  val analogController: LynxAnalogInputController by LazyHardware {
    LynxAnalogInputController(RobotController.appContext, module)
  }

  /** Motor controller for [port], claiming the port. */
  internal fun motor(port: Int): DcMotorImplEx {
    claim(PortKind.MOTOR, port)
    return DcMotorImplEx(motorController, port)
  }

  /** Servo controller for [port], claiming the port. */
  internal fun servo(port: Int): ServoImplEx {
    claim(PortKind.SERVO, port)
    return ServoImplEx(servoController, port, ServoConfigurationType.getStandardServoType())
  }

  /** CRServo controller for [port], claiming the port. */
  internal fun crServo(port: Int): CRServoImplEx {
    claim(PortKind.SERVO, port)
    return CRServoImplEx(servoController, port, ServoConfigurationType.getStandardServoType())
  }

  /** Creates a [LynxI2cColorRangeSensor] on [bus], claiming the bus. */
  internal fun colorRangeSensor(bus: Int): LynxI2cColorRangeSensor {
    claim(PortKind.I2C, bus)
    return LynxI2cColorRangeSensor(i2cController(bus), true)
  }

  /** Creates a [GoBildaPinpointDriver] on [bus], claiming the bus. */
  internal fun pinpoint(bus: Int): GoBildaPinpointDriver {
    claim(PortKind.I2C, bus)
    return GoBildaPinpointDriver(i2cController(bus), true)
  }

  /** Digital controller for [port], claiming the port. */
  internal fun digitalChannel(port: Int): DigitalChannel {
    claim(PortKind.DIGITAL, port)
    return DigitalChannelImpl(digitalController, port)
  }

  /** Analog controller for [port], claiming the port. */
  internal fun analogChannel(port: Int): AnalogInput {
    claim(PortKind.ANALOG, port)
    return AnalogInput(analogController, port)
  }
}
