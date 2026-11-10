# Module NextFTC Util

Small pieces shared by the other NextFTC modules. It has no FTC SDK or Android dependency, so every
module, including the pure-JVM ones, can depend on it.

# Package dev.nextftc.util

`EventLoop`, a lightweight loop that polls bound actions, and `Log`, a logging facade. `Log` prints
warnings and errors to standard error until something installs a `LogSink`; `hardware`'s
`RobotController` installs one that writes to the Robot Controller log.

# Package dev.nextftc.util.struct

`StructSchema`, the description of a WPILib struct (type name, fixed size, schema text, nested
types, and a packer), and `structSchema`, a builder for defining one by hand.
`StructSchemaRegistry` builds schemas by reflecting over a class's properties, or a Java class's
public fields, getters, or record components, with `StructName`, `StructIgnore`, `StructSize` and
related annotations to adjust the result.
