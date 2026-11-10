package dev.nextftc.util

/**
 * A lightweight execution loop for polling boolean triggers and executing bound actions.
 *
 * Functions similarly to WPILib's EventLoop, keeping a registry of Runnables that are
 * executed sequentially on every [poll] call.
 */
class EventLoop {
  private val bindings = mutableListOf<Runnable>()

  /**
   * Binds a new action to be executed every time the loop is polled.
   *
   * @param action The action to run.
   */
  fun bind(action: Runnable) {
    bindings.add(action)
  }

  /**
   * Executes all bound actions sequentially.
   */
  fun poll() {
    bindings.forEach { it.run() }
  }

  /**
   * Clears all bound actions from the loop.
   */
  fun clear() {
    bindings.clear()
  }
}
