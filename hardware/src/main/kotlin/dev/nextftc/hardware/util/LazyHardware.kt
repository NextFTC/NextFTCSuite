package dev.nextftc.hardware.util

import android.util.Log
import dev.nextftc.functionalInterfaces.Configurator
import dev.nextftc.hardware.RobotController
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Lazily initializes a hardware object on first access.
 *
 * The cached object is discarded when an OpMode stops (see [RobotController]), so the next
 * access re-runs the initializer against the new hardware map. Blocks passed to [applyAfterInit]
 * are re-applied on every initialization.
 */
class LazyHardware<T>(private val initializer: () -> T) : ReadOnlyProperty<Any?, T> {

  private var value: T? = null
  internal val isInitialized: Boolean
    get() = value != null

  private val onInit = LinkedHashMap<Any, Configurator<T>>()
  private val onStop = mutableListOf<() -> Unit>()

  init {
    RobotController.register(this)
  }

  override fun getValue(thisRef: Any?, property: KProperty<*>): T {
    if (value != null) return value!!

    return initializer.invoke().also { hardwareObject ->
      value = hardwareObject
      onInit.values.toList().forEach { block -> block.configure(hardwareObject) }
      Log.d(
        "NextFTC",
        "Initialized lazy $hardwareObject in property ${property.name} in class ${thisRef?.let {
          it::class.simpleName
        } ?: "Unknown"}",
      )
    }
  }

  /**
   * Runs [block] on the hardware object now if it is initialized, and again after every
   * (re-)initialization.
   */
  fun applyAfterInit(block: Configurator<T>) = applyAfterInit(Any(), block)

  /**
   * Like [applyAfterInit], but replaces any earlier block registered with the same [key], so
   * repeatedly updating one setting does not accumulate blocks.
   */
  fun applyAfterInit(key: Any, block: Configurator<T>) {
    onInit[key] = block
    value?.let { block.configure(it) }
  }

  /**
   * Runs [block] every time the cached object is discarded (for example when an OpMode stops).
   * Use this to invalidate state that mirrors the hardware, such as write caches.
   */
  fun onOpModeStop(block: () -> Unit) {
    onStop += block
  }

  /**
   * Discards the cached object so the next access re-initializes it.
   */
  internal fun reset() {
    value = null
    onStop.toList().forEach { it() }
  }
}
