package dev.nextftc.robot.triggers

import com.qualcomm.robotcore.hardware.Gamepad

/**
 * A wrapper around the standard FTC [Gamepad] that exposes its buttons as command-based [Trigger]s.
 *
 * This allows you to easily map controller inputs directly to Ivy commands using the WPILib-style
 * Trigger API (e.g., `gamepad.a.onTrue(scoreCommand)`).
 *
 * @param eventLoop The event loop to bind the button triggers to.
 * @param gamepad The underlying FTC gamepad instance to read data from.
 */
class CommandGamepad(private val eventLoop: EventLoop, private val gamepad: Gamepad) {
  /** The X value of the left joystick. */
  @get:JvmName("leftStickX")
  val leftStickX get() = gamepad.left_stick_x

  /** The Y value of the left joystick. */
  @get:JvmName("leftStickY")
  val leftStickY get() = gamepad.left_stick_y

  /** The X value of the right joystick. */
  @get:JvmName("rightStickX")
  val rightStickX get() = gamepad.right_stick_x

  /** The Y value of the right joystick. */
  @get:JvmName("rightStickY")
  val rightStickY get() = gamepad.right_stick_y

  /** A trigger that evaluates to true when the A button (cross on PlayStation) is pressed. */
  @get:JvmName("a")
  val a = Trigger(eventLoop) { gamepad.a }

  /** A trigger that evaluates to true when the B button (circle on PlayStation) is pressed. */
  @get:JvmName("b")
  val b = Trigger(eventLoop) { gamepad.b }

  /** A trigger that evaluates to true when the X button (square on PlayStation) is pressed. */
  @get:JvmName("x")
  val x = Trigger(eventLoop) { gamepad.x }

  /** A trigger that evaluates to true when the Y button (triangle on PlayStation) is pressed. */
  @get:JvmName("y")
  val y = Trigger(eventLoop) { gamepad.y }

  /** A trigger that evaluates to true when the left bumper is pressed. */
  @get:JvmName("leftBumper")
  val leftBumper = Trigger(eventLoop) { gamepad.left_bumper }

  /** A trigger that evaluates to true when the right bumper is pressed. */
  @get:JvmName("rightBumper")
  val rightBumper = Trigger(eventLoop) { gamepad.right_bumper }

  /** A trigger that evaluates to true when the d-pad up is pressed. */
  @get:JvmName("dpadUp")
  val dpadUp = Trigger(eventLoop) { gamepad.dpad_up }

  /** A trigger that evaluates to true when the d-pad down is pressed. */
  @get:JvmName("dpadDown")
  val dpadDown = Trigger(eventLoop) { gamepad.dpad_down }

  /** A trigger that evaluates to true when the d-pad left is pressed. */
  @get:JvmName("dpadLeft")
  val dpadLeft = Trigger(eventLoop) { gamepad.dpad_left }

  /** A trigger that evaluates to true when the d-pad right is pressed. */
  @get:JvmName("dpadRight")
  val dpadRight = Trigger(eventLoop) { gamepad.dpad_right }

  /** A trigger that evaluates to true when the back button is pressed. */
  @get:JvmName("back")
  val back = Trigger(eventLoop) { gamepad.back }

  /** A trigger that evaluates to true when the start button is pressed. */
  @get:JvmName("start")
  val start = Trigger(eventLoop) { gamepad.start }

  /** A trigger that evaluates to true when the left stick is clicked. */
  @get:JvmName("leftStickButton")
  val leftStickButton = Trigger(eventLoop) { gamepad.left_stick_button }

  /** A trigger that evaluates to true when the right stick is clicked. */
  @get:JvmName("rightStickButton")
  val rightStickButton = Trigger(eventLoop) { gamepad.right_stick_button }

  /** A trigger that evaluates to true when the square button (X on Xbox) is pressed. */
  @get:JvmName("square")
  val square = Trigger(eventLoop) { gamepad.square }

  /** A trigger that evaluates to true when the circle button (B on Xbox) is pressed. */
  @get:JvmName("circle")
  val circle = Trigger(eventLoop) { gamepad.circle }

  /** A trigger that evaluates to true when the cross button (A on Xbox) is pressed. */
  @get:JvmName("cross")
  val cross = Trigger(eventLoop) { gamepad.cross }

  /** A trigger that evaluates to true when the triangle button (Y on Xbox) is pressed. */
  @get:JvmName("triangle")
  val triangle = Trigger(eventLoop) { gamepad.triangle }
}
