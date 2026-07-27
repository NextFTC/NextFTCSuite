package dev.nextftc.hardware.util

import android.util.Log
import com.qualcomm.robotcore.eventloop.opmode.OpMode
import dev.nextftc.functionalInterfaces.Configurator
import dev.nextftc.hardware.RobotController
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class LazyHardware<T>(private val initializer: () -> T) : ReadOnlyProperty<Any?, T> {

  private var value: T? = null
  internal val isInitialized: Boolean
    get() = value != null

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

  private val onInit = mutableListOf<Configurator<T>>()

  fun applyAfterInit(block: Configurator<T>) {
    if (value != null) {
      block.configure(value)
    } else {
      onInit += block
    }
  }
}
