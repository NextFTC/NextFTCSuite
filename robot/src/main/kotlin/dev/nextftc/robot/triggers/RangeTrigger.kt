package dev.nextftc.robot.triggers

import dev.nextftc.util.EventLoop

/**
 * A wrapper around an analog gamepad input (e.g. a trigger  or joystick) that exposes
 * range-based [Trigger]s in addition to the raw value.
 *
 * <p>This allows you to bind analog inputs to command-based [Trigger]s based on their value,
 * e.g. `gamepad.rightTrigger.isOver(0.5).onTrue(intakeCommand)`.
 */
class RangeTrigger @JvmOverloads constructor(
  private val loop: EventLoop = Trigger.defaultEventLoop,
  private val supplier: () -> Double,
) : () -> Double {

  /**
   * The current value of the trigger.
   */
  val value: Double get() = supplier()

  override fun invoke(): Double = supplier()

  /**
   * Creates a trigger that is active while this input's value is greater than [threshold].
   */
  fun isOver(threshold: Double): Trigger = Trigger(loop) { supplier() > threshold }

  /**
   * Creates a trigger that is active while this input's value is less than [threshold].
   */
  fun isUnder(threshold: Double): Trigger = Trigger(loop) { supplier() < threshold }

  /**
   * Creates a trigger that is active while this input's value is within [lower, upper] (inclusive).
   */
  fun isBetween(lower: Double, upper: Double): Trigger = Trigger(loop) { supplier() in lower..upper }

  /**
   * Creates a trigger that is active while this input's value is within [range] (inclusive).
   */
  fun isBetween(range: ClosedFloatingPointRange<Double>): Trigger = Trigger(loop) { supplier() in range }
}
