# Module NextControl Units

A type-safe units library for FTC robot code, modeled after WPILib's units library. Distances,
times, masses, forces, torques, energy, power, current, voltage, temperature, and angles (plus
their velocity and acceleration derivatives) are all represented as `Measure` values tied to `Unit`
definitions, instead of raw doubles that leave you guessing what unit they're in.

Measures support arithmetic and comparisons against each other, convert between units explicitly,
and combine into compound units automatically when you multiply or divide (e.g. `Distance / Time`
gives you a `LinearVelocity`). Extension properties like `5.0.meters` and `10.0.rpm` keep literals
readable.

# Package dev.nextftc.units

The `Measure` and `Unit` base types, plus the `Units` object with the built-in unit constants
(`Meters`, `Seconds`, `Radians`, `Volts`, etc.) and their `Double` extension properties.

# Package dev.nextftc.units.measuretypes

The concrete `Measure` types: `Distance`, `Time`, `Angle`, `Mass`, `Force`, `Torque`, `Energy`,
`Power`, `Current`, `Voltage`, `Temperature`, `LinearVelocity`/`LinearAcceleration`, and
`AngularVelocity`/`AngularAcceleration`. Also includes `Mul` and `Per`, the generic compound types
that back cross-quantity arithmetic.

# Package dev.nextftc.units.unittypes

The `Unit` subclass for each measure type, defining its conversion factors and `of`/`ofBaseUnits`
factories. Also includes the generic `MulUnit`/`PerUnit` compound units and specializations like
`LinearVelocityUnit`.
