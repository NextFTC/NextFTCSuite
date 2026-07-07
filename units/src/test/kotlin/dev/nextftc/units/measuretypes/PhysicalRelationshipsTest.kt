/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.units.measuretypes

import dev.nextftc.units.Amperes
import dev.nextftc.units.Centimeters
import dev.nextftc.units.Feet
import dev.nextftc.units.Horsepower
import dev.nextftc.units.Hours
import dev.nextftc.units.Inches
import dev.nextftc.units.Joules
import dev.nextftc.units.Kilograms
import dev.nextftc.units.Kilojoules
import dev.nextftc.units.Kilonewtons
import dev.nextftc.units.Kilovolts
import dev.nextftc.units.Kilowatts
import dev.nextftc.units.Meters
import dev.nextftc.units.Milliamperes
import dev.nextftc.units.Millivolts
import dev.nextftc.units.Minutes
import dev.nextftc.units.NewtonMeters
import dev.nextftc.units.Newtons
import dev.nextftc.units.PoundFeet
import dev.nextftc.units.Pounds
import dev.nextftc.units.PoundsForce
import dev.nextftc.units.Radians
import dev.nextftc.units.Seconds
import dev.nextftc.units.Volts
import dev.nextftc.units.WattHours
import dev.nextftc.units.Watts
import dev.nextftc.units.unittypes.AngularVelocityUnit
import dev.nextftc.units.unittypes.LinearAccelerationUnit
import dev.nextftc.units.unittypes.LinearVelocityUnit
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

private const val EPSILON = 1e-9

class PhysicalRelationshipsTest :
  FunSpec({
    context("Force × Distance = Energy (Work)") {
      test("1 N × 1 m = 1 J") {
        val force = Newtons.of(1.0)
        val distance = Meters.of(1.0)
        val energy = force * distance

        energy.magnitude shouldBe (1.0 plusOrMinus EPSILON)
        energy.unit shouldBeSameInstanceAs Joules
      }

      test("10 N × 5 m = 50 J") {
        val force = Newtons.of(10.0)
        val distance = Meters.of(5.0)
        val energy = force * distance

        energy.magnitude shouldBe (50.0 plusOrMinus EPSILON)
      }

      test("force in kilonewtons × distance in centimeters = correct joules") {
        val force = Kilonewtons.of(1.0) // 1000 N
        val distance = Centimeters.of(100.0) // 1 m
        val energy = force * distance

        energy.baseUnitMagnitude shouldBe (1000.0 plusOrMinus EPSILON)
      }

      test("Distance × Force is commutative") {
        val force = Newtons.of(15.0)
        val distance = Meters.of(3.0)

        val energy1 = force * distance
        val energy2 = distance * force

        energy1.baseUnitMagnitude shouldBe (energy2.baseUnitMagnitude plusOrMinus EPSILON)
      }
    }

    context("Energy / Time = Power") {
      test("1 J / 1 s = 1 W") {
        val energy = Joules.of(1.0)
        val time = Seconds.of(1.0)
        val power = energy / time

        power.magnitude shouldBe (1.0 plusOrMinus EPSILON)
        power.unit shouldBeSameInstanceAs Watts
      }

      test("1000 J / 10 s = 100 W") {
        val energy = Joules.of(1000.0)
        val time = Seconds.of(10.0)
        val power = energy / time

        power.magnitude shouldBe (100.0 plusOrMinus EPSILON)
      }

      test("energy in kilojoules / time in minutes = correct watts") {
        val energy = Kilojoules.of(60.0) // 60,000 J
        val time = Minutes.of(1.0) // 60 s
        val power = energy / time

        power.baseUnitMagnitude shouldBe (1000.0 plusOrMinus EPSILON)
      }
    }

    context("Power × Time = Energy") {
      test("1 W × 1 s = 1 J") {
        val power = Watts.of(1.0)
        val time = Seconds.of(1.0)
        val energy = power * time

        energy.magnitude shouldBe (1.0 plusOrMinus EPSILON)
        energy.unit shouldBeSameInstanceAs Joules
      }

      test("100 W × 60 s = 6000 J") {
        val power = Watts.of(100.0)
        val time = Seconds.of(60.0)
        val energy = power * time

        energy.magnitude shouldBe (6000.0 plusOrMinus EPSILON)
      }

      test("power in kilowatts × time in hours = correct joules") {
        val power = Kilowatts.of(1.0) // 1000 W
        val time = Hours.of(1.0) // 3600 s
        val energy = power * time

        energy.baseUnitMagnitude shouldBe (3_600_000.0 plusOrMinus EPSILON)
      }

      test("Power × Time and Energy / Time are inverse operations") {
        val originalPower = Watts.of(500.0)
        val time = Seconds.of(10.0)

        val energy = originalPower * time
        val recoveredPower = energy / time

        recoveredPower.baseUnitMagnitude shouldBe
          (originalPower.baseUnitMagnitude plusOrMinus EPSILON)
      }
    }

    context("Voltage × Current = Power") {
      test("1 V × 1 A = 1 W") {
        val voltage = Volts.of(1.0)
        val current = Amperes.of(1.0)
        val power = voltage * current

        power.magnitude shouldBe (1.0 plusOrMinus EPSILON)
        power.unit shouldBeSameInstanceAs Watts
      }

      test("12 V × 2 A = 24 W") {
        val voltage = Volts.of(12.0)
        val current = Amperes.of(2.0)
        val power = voltage * current

        power.magnitude shouldBe (24.0 plusOrMinus EPSILON)
      }

      test("voltage in millivolts × current in milliamperes = correct watts") {
        val voltage = Millivolts.of(5000.0) // 5 V
        val current = Milliamperes.of(2000.0) // 2 A
        val power = voltage * current

        power.baseUnitMagnitude shouldBe (10.0 plusOrMinus EPSILON)
      }

      test("Current × Voltage is commutative") {
        val voltage = Volts.of(24.0)
        val current = Amperes.of(3.0)

        val power1 = voltage * current
        val power2 = current * voltage

        power1.baseUnitMagnitude shouldBe (power2.baseUnitMagnitude plusOrMinus EPSILON)
      }
    }

    context("Mass × Acceleration = Force (F = ma)") {
      test("1 kg × 1 m/s² = 1 N") {
        val mass = Kilograms.of(1.0)
        val acceleration = LinearAccelerationUnit(
          LinearVelocityUnit(Meters, Seconds),
          Seconds,
        ).of(1.0)
        val force = mass * acceleration

        force.magnitude shouldBe (1.0 plusOrMinus EPSILON)
        force.unit shouldBeSameInstanceAs Newtons
      }

      test("10 kg × 9.81 m/s² ≈ 98.1 N (weight on Earth)") {
        val mass = Kilograms.of(10.0)
        val gravity = LinearAccelerationUnit(
          LinearVelocityUnit(Meters, Seconds),
          Seconds,
        ).of(9.81)
        val weight = mass * gravity

        weight.magnitude shouldBe (98.1 plusOrMinus 0.01)
      }

      test("Acceleration × Mass is commutative") {
        val mass = Kilograms.of(5.0)
        val acceleration = LinearAccelerationUnit(
          LinearVelocityUnit(Meters, Seconds),
          Seconds,
        ).of(2.0)

        val force1 = mass * acceleration
        val force2 = acceleration * mass

        force1.baseUnitMagnitude shouldBe (force2.baseUnitMagnitude plusOrMinus EPSILON)
      }
    }

    context("Torque / Distance = Force") {
      test("1 N·m / 1 m = 1 N") {
        val torque = NewtonMeters.of(1.0)
        val distance = Meters.of(1.0)
        val force = torque / distance

        force.magnitude shouldBe (1.0 plusOrMinus EPSILON)
        force.unit shouldBeSameInstanceAs Newtons
      }

      test("100 N·m / 0.5 m = 200 N") {
        val torque = NewtonMeters.of(100.0)
        val distance = Meters.of(0.5)
        val force = torque / distance

        force.magnitude shouldBe (200.0 plusOrMinus EPSILON)
      }

      test("torque in pound-feet / distance in inches = correct newtons") {
        val torque = PoundFeet.of(1.0)
        val distance = Inches.of(12.0) // 1 foot = 0.3048 m
        val force = torque / distance

        // 1 lb·ft ≈ 1.356 N·m, 12 in = 0.3048 m
        // Force = 1.356 / 0.3048 ≈ 4.45 N
        force.baseUnitMagnitude shouldBe (4.45 plusOrMinus 0.01)
      }
    }

    context("Torque × AngularVelocity = Power") {
      test("1 N·m × 1 rad/s = 1 W") {
        val torque = NewtonMeters.of(1.0)
        val angularVelocity = AngularVelocityUnit(Radians, Seconds).of(1.0)
        val power = torque * angularVelocity

        power.magnitude shouldBe (1.0 plusOrMinus EPSILON)
        power.unit shouldBeSameInstanceAs Watts
      }

      test("10 N·m × 100 rad/s = 1000 W") {
        val torque = NewtonMeters.of(10.0)
        val angularVelocity = AngularVelocityUnit(Radians, Seconds).of(100.0)
        val power = torque * angularVelocity

        power.magnitude shouldBe (1000.0 plusOrMinus EPSILON)
      }

      test("AngularVelocity × Torque is commutative") {
        val torque = NewtonMeters.of(50.0)
        val angularVelocity = AngularVelocityUnit(Radians, Seconds).of(20.0)

        val power1 = torque * angularVelocity
        val power2 = angularVelocity * torque

        power1.baseUnitMagnitude shouldBe (power2.baseUnitMagnitude plusOrMinus EPSILON)
      }
    }

    context("Force.timesTorque() for moment arm calculations") {
      test("1 N at 1 m = 1 N·m torque") {
        val force = Newtons.of(1.0)
        val momentArm = Meters.of(1.0)
        val torque = force.timesTorque(momentArm)

        torque.magnitude shouldBe (1.0 plusOrMinus EPSILON)
        torque.unit shouldBeSameInstanceAs NewtonMeters
      }

      test("50 N at 0.2 m = 10 N·m torque") {
        val force = Newtons.of(50.0)
        val momentArm = Meters.of(0.2)
        val torque = force.timesTorque(momentArm)

        torque.magnitude shouldBe (10.0 plusOrMinus EPSILON)
      }
    }

    context("Unit preservation and base unit conversion consistency") {
      test("operations should always convert to base units for calculation") {
        // 1 kN × 1 cm should give the same result as 1000 N × 0.01 m
        val energy1 = Kilonewtons.of(1.0) * Centimeters.of(1.0)
        val energy2 = Newtons.of(1000.0) * Meters.of(0.01)

        energy1.baseUnitMagnitude shouldBe (energy2.baseUnitMagnitude plusOrMinus EPSILON)
      }

      test("chain of operations maintains consistency") {
        // Power = Force × Velocity = Force × Distance / Time
        val force = Newtons.of(100.0)
        val distance = Meters.of(10.0)
        val time = Seconds.of(2.0)

        val energy = force * distance // 1000 J
        val power = energy / time // 500 W

        power.baseUnitMagnitude shouldBe (500.0 plusOrMinus EPSILON)
      }
    }

    context("Imperial unit conversions in physical relationships") {
      test("pounds-force × feet = correct energy in joules") {
        val force = PoundsForce.of(1.0) // 4.448 N
        val distance = Feet.of(1.0) // 0.3048 m
        val energy = force * distance

        // 1 lbf × 1 ft ≈ 1.356 J (foot-pound)
        energy.baseUnitMagnitude shouldBe (1.3558 plusOrMinus 0.001)
      }

      test("pounds-force × inches = correct energy") {
        val force = PoundsForce.of(12.0)
        val distance = Inches.of(6.0) // 0.5 ft = 0.1524 m
        val energy = force * distance

        // 12 lbf × 6 in = 12 × 4.448 N × 0.1524 m ≈ 8.13 J
        energy.baseUnitMagnitude shouldBe (8.135 plusOrMinus 0.01)
      }

      test("pound-feet torque / feet = pounds-force") {
        val torque = PoundFeet.of(10.0)
        val distance = Feet.of(2.0)
        val force = torque / distance

        // 10 lb·ft / 2 ft = 5 lbf ≈ 22.24 N
        force.baseUnitMagnitude shouldBe (22.24 plusOrMinus 0.01)
      }

      test("mass in pounds × acceleration = force") {
        val mass = Pounds.of(10.0) // ~4.536 kg
        val acceleration = LinearAccelerationUnit(
          LinearVelocityUnit(Meters, Seconds),
          Seconds,
        ).of(9.81) // m/s²
        val force = mass * acceleration

        // 4.536 kg × 9.81 m/s² ≈ 44.5 N
        force.baseUnitMagnitude shouldBe (44.5 plusOrMinus 0.1)
      }

      test("horsepower × time = energy") {
        val power = Horsepower.of(1.0) // ~745.7 W
        val time = Seconds.of(1.0)
        val energy = power * time

        // 1 hp × 1 s ≈ 745.7 J
        energy.baseUnitMagnitude shouldBe (745.7 plusOrMinus 0.1)
      }

      test("energy in watt-hours / time in hours = power in watts") {
        val energy = WattHours.of(100.0) // 360,000 J
        val time = Hours.of(2.0) // 7200 s
        val power = energy / time

        // 100 Wh / 2 h = 50 W
        power.baseUnitMagnitude shouldBe (50.0 plusOrMinus EPSILON)
      }

      test("kilowatt-hours energy calculation") {
        val power = Kilowatts.of(2.0) // 2000 W
        val time = Hours.of(3.0) // 10800 s
        val energy = power * time

        // 2 kW × 3 h = 6 kWh = 21,600,000 J
        energy.baseUnitMagnitude shouldBe (21_600_000.0 plusOrMinus 1.0)
      }
    }

    context("Mixed metric and imperial calculations") {
      test("force in newtons × distance in feet = correct joules") {
        val force = Newtons.of(100.0)
        val distance = Feet.of(1.0) // 0.3048 m
        val energy = force * distance

        energy.baseUnitMagnitude shouldBe (30.48 plusOrMinus 0.01)
      }

      test("force in pounds-force × distance in meters = correct joules") {
        val force = PoundsForce.of(1.0) // 4.448 N
        val distance = Meters.of(1.0)
        val energy = force * distance

        energy.baseUnitMagnitude shouldBe (4.448 plusOrMinus 0.001)
      }

      test("torque in newton-meters / distance in inches = correct force") {
        val torque = NewtonMeters.of(1.0)
        val distance = Inches.of(1.0) // 0.0254 m
        val force = torque / distance

        // 1 N·m / 0.0254 m ≈ 39.37 N
        force.baseUnitMagnitude shouldBe (39.37 plusOrMinus 0.01)
      }

      test("voltage in kilovolts × current in milliamperes = correct power") {
        val voltage = Kilovolts.of(1.0) // 1000 V
        val current = Milliamperes.of(500.0) // 0.5 A
        val power = voltage * current

        power.baseUnitMagnitude shouldBe (500.0 plusOrMinus EPSILON)
      }
    }
  })
