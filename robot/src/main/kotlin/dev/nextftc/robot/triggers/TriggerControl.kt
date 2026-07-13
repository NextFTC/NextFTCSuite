package dev.nextftc.robot.triggers

import dev.nextftc.hardware.util.EventLoop
import dev.nextftc.robot.triggers.Trigger

/**
 * A easy way to use triggers for gamepad
 */
class TriggerControl(private val loop: EventLoop, private val supplier: () -> Double) : () -> Double {

  /**
   * Creates a new trigger control
   */
  constructor(supplier: () -> Double) : this(Trigger.defaultEventLoop, supplier)

  val value: Double get() = supplier()

  override fun invoke(): Double = supplier()

  fun getAsDouble(): Double = supplier()

  /**
   * Creates a trigger that is active while this input's value is greater than [threshold].
   */
  fun isOver(threshold: Double): Trigger = isBetween(threshold, 1.0)

  /**
   * Creates a trigger that is active while this input's value is  less than [threshold].
   */
  fun isUnder(threshold: Double): Trigger = isBetween(0.0, threshold)

  /**
   * Creates a trigger that is active while this input's value is within [lower, upper] (inclusive).
   */
  fun isBetween(lower: Double, upper: Double): Trigger = Trigger(loop) { supplier() in lower..upper }
}
