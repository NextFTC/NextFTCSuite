/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.telemetry.struct

import java.lang.reflect.Method
import java.lang.reflect.Modifier
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.KVisibility
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.isAccessible

/**
 * Finds or builds a [StructSchema] for a class, reflecting over its fields.
 *
 * A registry reflects a class the first time it is asked for it, then keeps the result, so every
 * request for the same class returns the same schema. Classes nested in its fields are reflected
 * too, and each becomes a nested struct. Give each logger its own registry, or share one.
 *
 * ## What is reflected
 *
 * A Kotlin class contributes its public properties. A Java class contributes its public fields and
 * its getters, which are public no-argument instance methods named `getX`, or `isX` returning a
 * boolean, and the field is named `x`. A Java record contributes its components. A field that is
 * both a public field and a getter is read from the field.
 *
 * Each field is named after its property, field, or getter, with these types:
 * - `Boolean`, `Byte`, `Short`, `Int`, `Long`, `Float` and `Double`, and the Java primitives and
 *   boxes for them
 * - `DoubleArray`, which needs [StructSize] to declare its length
 * - any other concrete class, as a nested struct
 *
 * ## Order
 *
 * The order is the layout on the wire, so changing it changes the schema:
 * - A Kotlin class uses primary constructor order, then any remaining properties alphabetically.
 * - A Java record uses component order.
 * - Any other class uses alphabetical order, because JVM bytecode doesn't keep declaration order for
 *   methods.
 *
 * [StructOrder] overrides this for any class. Fields marked [StructIgnore] are skipped, including
 * computed ones you don't want logged.
 *
 * Nullable properties, strings, collections, enums, and platform types have no struct encoding and
 * are rejected with an error that names the field. Cyclic nesting is rejected too. For such a type,
 * write a schema by hand with [structSchema] and [register] it, or mark the field [StructIgnore].
 *
 * The struct's name is the class's simple name, or the value of [StructName] on it.
 *
 * Reading fields by reflection boxes each value, so it suits a few structs logged each loop. For a
 * hot path, use a hand-written schema.
 *
 * Methods are safe to call from multiple threads.
 */
class StructSchemaRegistry {
  private val schemas = HashMap<KClass<*>, StructSchema<*>>()

  /**
   * Uses [schema] for [type], both when it is requested directly and when it appears as a field of
   * a reflected class. Use it for a class you can't annotate or that reflection can't handle.
   *
   * @throws IllegalArgumentException if [type] already has a schema, whether registered or reflected
   */
  @Synchronized
  fun <T : Any> register(type: KClass<T>, schema: StructSchema<T>) {
    require(type !in schemas) {
      "${type.qualifiedName} already has a struct schema, so it can't be registered again"
    }
    schemas[type] = schema
  }

  /**
   * Returns the schema for [type], registered or reflected.
   *
   * @throws IllegalArgumentException if [type] or something nested in it can't be a struct
   */
  @Suppress("UNCHECKED_CAST")
  @Synchronized
  fun <T : Any> schemaOf(type: KClass<T>): StructSchema<T> =
    resolve(type, ArrayDeque(), "$type") as StructSchema<T>

  /** Returns the schema for [type]. See [schemaOf]. */
  fun <T : Any> schemaOf(type: Class<T>): StructSchema<T> = schemaOf(type.kotlin)

  /** Returns the schema for [T]. See [schemaOf]. */
  inline fun <reified T : Any> schemaOf(): StructSchema<T> = schemaOf(T::class)

  /**
   * Returns the cached schema for [type], or reflects it.
   *
   * @param path The classes being reflected, outermost first, to catch cycles
   * @param context What asked for [type], for error messages
   */
  private fun resolve(type: KClass<*>, path: ArrayDeque<KClass<*>>, context: String): StructSchema<*> {
    schemas[type]?.let {
      return it
    }
    unsupportedReason(type)?.let {
      throw IllegalArgumentException(
        "$context can't be a struct: ${type.qualifiedName ?: type.java.name} $it. Register a " +
          "hand-written schema with register(), or mark the field @StructIgnore.",
      )
    }
    require(type !in path) {
      "Struct nesting is cyclic: " + (path + type).joinToString(" -> ") { it.simpleName ?: "?" }
    }
    path.addLast(type)
    try {
      return reflect(type, path).also { schemas[type] = it }
    } finally {
      path.removeLast()
    }
  }

  private fun unsupportedReason(type: KClass<*>): String? {
    val java = type.java
    return when {
      java.isPrimitive -> "is a primitive with no struct encoding"
      java.name.startsWith("java.") || java.name.startsWith("kotlin.") ->
        "is a platform type with no struct encoding"
      java.isEnum -> "is an enum, which isn't supported"
      java.isArray -> "is an array, which isn't supported here"
      java.isInterface || type.isAbstract -> "is not a concrete class"
      else -> null
    }
  }

  private fun <T : Any> reflect(type: KClass<T>, path: ArrayDeque<KClass<*>>): StructSchema<T> {
    val name =
      type.findAnnotation<StructName>()?.value
        ?: type.simpleName
        ?: throw IllegalArgumentException(
          "An anonymous class can't be a struct. Give it a name, or register a schema for it.",
        )
    val owner = type.qualifiedName ?: type.java.name
    val members = structMembers(type, owner)
    return structSchema(name) { for (member in members) declare(member, owner, path) }
  }

  private fun <T : Any> StructSchemaBuilder<T>.declare(
    member: Member,
    ownerName: String,
    path: ArrayDeque<KClass<*>>,
  ) {
    val name = member.name
    val where = "$ownerName.$name"
    require(!member.nullable) {
      "$where is nullable, but a struct has no encoding for null. Make it non-null or mark it " +
        "@StructIgnore."
    }

    fun read(target: T): Any =
      member.read(target) ?: throw IllegalStateException("$where is null, so it can't be packed")

    when (val type = member.type) {
      Boolean::class -> boolean(name) { read(it) as Boolean }
      Byte::class -> int8(name) { read(it) as Byte }
      Short::class -> int16(name) { read(it) as Short }
      Int::class -> int32(name) { read(it) as Int }
      Long::class -> int64(name) { read(it) as Long }
      Float::class -> float32(name) { read(it) as Float }
      Double::class -> float64(name) { read(it) as Double }
      DoubleArray::class -> {
        val length =
          member.length
            ?: throw IllegalArgumentException(
              "$where is a DoubleArray, so it needs @StructSize(n) to declare its fixed length.",
            )
        float64Array(name, length) { read(it) as DoubleArray }
      }
      null ->
        throw IllegalArgumentException(
          "$where has type ${member.typeName}, which can't be a struct field.",
        )
      else -> {
        @Suppress("UNCHECKED_CAST")
        val nested = resolve(type, path, where) as StructSchema<Any>
        struct(name, nested) { read(it) }
      }
    }
  }

  /** One field of a struct, found by reflection, whatever it was read from. */
  private class Member(
    val name: String,
    /** The field's class, or null if it isn't one, such as a type parameter. */
    val type: KClass<*>?,
    val typeName: String,
    val nullable: Boolean,
    val ignored: Boolean,
    val length: Int?,
    val read: (Any) -> Any?,
  )

  /** The fields of [type] that go in the struct, in wire order. */
  private fun structMembers(type: KClass<*>, owner: String): List<Member> {
    val isKotlin = type.java.isAnnotationPresent(Metadata::class.java)
    val recordAccessors = if (isKotlin) null else recordAccessors(type.java)

    val candidates: List<Member>
    val naturalOrder: List<String>
    when {
      isKotlin -> {
        candidates = propertyMembers(type)
        naturalOrder = type.primaryConstructor?.parameters?.mapNotNull { it.name }.orEmpty()
      }
      recordAccessors != null -> {
        candidates = recordAccessors.map { methodMember(it, it.name) }
        naturalOrder = recordAccessors.map { it.name }
      }
      else -> {
        val fields = propertyMembers(type)
        val fieldNames = fields.mapTo(HashSet()) { it.name }
        candidates = fields + getterMembers(type.java).filter { it.name !in fieldNames }
        naturalOrder = emptyList()
      }
    }

    val members = candidates.filterNot { it.ignored }
    val explicitOrder = type.findAnnotation<StructOrder>()?.value?.toList()
    explicitOrder?.forEach { name ->
      require(members.any { it.name == name }) {
        "@StructOrder on $owner lists '$name', which is not one of its fields " +
          "(${members.map { it.name }})"
      }
    }
    val order = explicitOrder ?: naturalOrder
    return members.sortedWith(
      compareBy(
        { order.indexOf(it.name).takeIf { index -> index >= 0 } ?: Int.MAX_VALUE },
        { it.name },
      ),
    )
  }

  /** The public properties of a Kotlin class, or the public fields of a Java one. */
  private fun propertyMembers(type: KClass<*>): List<Member> = type.memberProperties
    .filter { it.visibility == KVisibility.PUBLIC }
    .map { property ->
      property.isAccessible = true
      @Suppress("UNCHECKED_CAST")
      val readable = property as KProperty1<Any, *>
      Member(
        name = property.name,
        type = property.returnType.classifier as? KClass<*>,
        typeName = property.returnType.toString(),
        nullable = property.returnType.isMarkedNullable,
        ignored = property.hasAnnotation<StructIgnore>(),
        length = property.findAnnotation<StructSize>()?.value,
        read = { readable.get(it) },
      )
    }

  private fun getterMembers(java: Class<*>): List<Member> = java.methods
    .mapNotNull { method -> getterName(method)?.let { it to method } }
    .sortedBy { it.first }
    .distinctBy { it.first }
    .map { (name, method) -> methodMember(method, name) }

  private fun methodMember(method: Method, name: String): Member {
    method.setAccessible(true)
    return Member(
      name = name,
      type = method.returnType.kotlin,
      typeName = method.genericReturnType.typeName,
      nullable = false,
      ignored = method.isAnnotationPresent(StructIgnore::class.java),
      length = method.getAnnotation(StructSize::class.java)?.value,
      read = { method.invoke(it) },
    )
  }

  /** The field name for [method] if it is a getter, or null. */
  private fun getterName(method: Method): String? {
    if (
      method.parameterTypes.isNotEmpty() ||
      Modifier.isStatic(method.modifiers) ||
      method.isSynthetic ||
      method.isBridge ||
      method.returnType == Void.TYPE ||
      method.declaringClass == Any::class.java
    ) {
      return null
    }
    val name = method.name
    val prefixLength =
      when {
        name.startsWith("get") -> 3
        name.startsWith("is") &&
          (
            method.returnType == Boolean::class.javaPrimitiveType ||
              method.returnType == Boolean::class.javaObjectType
            ) -> 2
        else -> return null
      }
    val rest = name.substring(prefixLength)
    if (rest.isEmpty() || !rest[0].isUpperCase()) return null
    // Follow the JavaBeans convention: getURL is "URL", but getName is "name".
    return if (rest.length > 1 && rest[1].isUpperCase()) {
      rest
    } else {
      rest[0].lowercaseChar() + rest.substring(1)
    }
  }

  /**
   * The accessor of each component of [java], in component order, or null if it isn't a record.
   *
   * Records need Java 16, and this module targets older versions, so this goes through reflection
   * instead of the record API.
   */
  private fun recordAccessors(java: Class<*>): List<Method>? {
    val isRecord =
      try {
        Class::class.java.getMethod("isRecord").invoke(java) as Boolean
      } catch (e: NoSuchMethodException) {
        false
      }
    if (!isRecord) return null
    val components = Class::class.java.getMethod("getRecordComponents").invoke(java) as Array<*>
    return components.map { component ->
      component!!.javaClass.getMethod("getAccessor").invoke(component) as Method
    }
  }
}
