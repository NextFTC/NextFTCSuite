/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.hardware

import dev.nextftc.functionalInterfaces.Configurator
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class LazyHardwareTest :
  FunSpec({
    test("initializer is not invoked until first access") {
      var invoked = false
      class Holder {
        val value by LazyHardware {
          invoked = true
          42
        }
      }
      val holder = Holder()
      invoked shouldBe false
      holder.value shouldBe 42
      invoked shouldBe true
    }

    test("initializer only runs once across repeated accesses") {
      var calls = 0
      class Holder {
        val value by LazyHardware { ++calls }
      }
      val holder = Holder()
      holder.value
      holder.value
      holder.value
      calls shouldBe 1
    }

    test("repeated access returns the same cached instance") {
      var counter = 0
      class Holder {
        val value by LazyHardware { ++counter }
      }
      val holder = Holder()
      val first = holder.value
      val second = holder.value
      first shouldBe second
    }

    test("applyAfterInit runs immediately when the value is already initialized") {
      lateinit var lazyRef: LazyHardware<String>
      class Holder {
        val value by LazyHardware { "device" }.also { lazyRef = it }
      }
      val holder = Holder()
      holder.value

      var configured: String? = null
      lazyRef.applyAfterInit(Configurator { configured = it })
      configured shouldBe "device"
    }

    test("applyAfterInit queues the callback until the value is initialized") {
      lateinit var lazyRef: LazyHardware<String>
      class Holder {
        val value by LazyHardware { "device" }.also { lazyRef = it }
      }
      val holder = Holder()

      var configured: String? = null
      lazyRef.applyAfterInit(Configurator { configured = it })
      configured shouldBe null

      holder.value
      configured shouldBe "device"
    }

    test("multiple queued callbacks all run once the value is initialized") {
      lateinit var lazyRef: LazyHardware<String>
      class Holder {
        val value by LazyHardware { "device" }.also { lazyRef = it }
      }
      val holder = Holder()
      val configuredValues = mutableListOf<String>()

      lazyRef.applyAfterInit(Configurator { configuredValues += it })
      lazyRef.applyAfterInit(Configurator { configuredValues += it })

      holder.value
      configuredValues shouldBe listOf("device", "device")
    }
  })
