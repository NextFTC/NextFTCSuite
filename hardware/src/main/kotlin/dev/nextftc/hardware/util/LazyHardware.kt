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

  private val onInit = mutableListOf<Configurator<T>>()

  init {
    RobotController.register(this)
  }

  override fun getValue(thisRef: Any?, property: KProperty<*>): T {
    if (value != null) return value!!

    return initializer.invoke().also { hardwareObject ->
      value = hardwareObject
      onInit.forEach { block -> block.configure(hardwareObject) }
      Log.d(
        "NextFTC",
        "Initialized lazy $hardwareObject in property ${property.name} in class ${thisRef?.let {
          it::class.simpleName
        } ?: "Unknown"}",
      )
    }
  }

  fun applyAfterInit(block: Configurator<T>) {
    onInit += block
    value?.let { block.configure(it) }
  }

  /**
   * Discards the cached object so the next access re-initializes it.
   */
  internal fun reset() {
    value = null
  }
}
