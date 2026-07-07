# Module NextControl Linear Algebra

A Kotlin wrapper around EJML's `SimpleMatrix` for NextControl's linear algebra needs. There are
two flavors: `Matrix<R, C>`/`Vector<N>`, which use `Nat` phantom types so dimension mismatches are
caught at compile time, and `DynamicMatrix`/`DynamicVector`, which check dimensions at runtime for
cases where the size isn't known ahead of time.

Both support the usual operations — arithmetic, transpose, inverse, pseudo-inverse, norm, solving
linear systems, matrix exponentiation — plus the common decompositions (Cholesky/LLT, LDLT, LU,
QR) and builder helpers (`buildMatrix`, `matrixOf`, `vectorOf`) for writing matrices and vectors as
literals. This module is the numerical backbone the `control` module's state-space and estimation
code is built on.

# Package dev.nextftc.linalg

Everything lives in one package: `Nat`/`N1`..`N10` for type-level dimensions, `Matrix`/`Vector`
(statically sized) and `DynamicMatrix`/`DynamicVector` (runtime sized), the decomposition result
types (`LLTDecomposition`, `LDLTDecomposition`, `LUDecomposition`, `QRDecomposition`), and the
`MatrixBuilder`/`matrixOf`/`vectorOf` construction helpers.
