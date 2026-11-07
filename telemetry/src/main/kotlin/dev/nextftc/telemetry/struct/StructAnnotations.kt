/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.telemetry.struct

/**
 * Sets the struct type name of a class when a [StructSchemaRegistry] reflects it, instead of the
 * class's simple name.
 *
 * AdvantageScope recognizes WPILib's built-in types by name, so a class meant to show up as a pose
 * or a rotation must use WPILib's name, such as `Pose2d`, and WPILib's field layout.
 */
@MustBeDocumented
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class StructName(val value: String)

/**
 * Leaves a property, field, or getter out of the struct when a [StructSchemaRegistry] reflects its
 * class.
 */
@MustBeDocumented
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class StructIgnore

/**
 * Declares the fixed length of an array property, field, or getter, which a struct needs and the
 * array's type can't say. Only `DoubleArray` (`double[]` in Java) is supported.
 */
@MustBeDocumented
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class StructSize(val value: Int)

/**
 * Sets the order of a class's struct fields when a [StructSchemaRegistry] reflects it. The order is
 * the layout on the wire.
 *
 * The listed names come first, in the order given. Any other fields follow alphabetically. A name
 * that isn't one of the class's fields is an error. This replaces the default order, which is
 * constructor order for a Kotlin class, component order for a Java record, and alphabetical for any
 * other Java class.
 */
@MustBeDocumented
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class StructOrder(vararg val value: String)
