# just-fp

[![Build Status](https://semaphoreci.com/api/v1/kevin-lee/just-fp/branches/master/badge.svg)](https://semaphoreci.com/kevin-lee/just-fp)
[![Download](https://api.bintray.com/packages/kevinlee/maven/just-fp/images/download.svg)](https://bintray.com/kevinlee/maven/just-fp/_latestVersion)

A small Functional Programming library. This is not meant to be an alternative to Scalaz or Cats. The reason for having this library is that in your project you don't want to have Scalaz or Cats as its dependency and you only need much smaller set of functional programming features than what Scalaz or Cats offers. So the users of your library can choose Scalaz or Cats to be used with your library.

# Getting Started
In `build.sbt`,

```sbt
resolvers += "Just Repo" at "https://dl.bintray.com/kevinlee/maven"

libraryDependencies += "kevinlee" %% "just-fp" % "1.2.0"
```
then import

```scala
import just.fp._
import just.fp.syntax._
```
or 
```scala
import just.fp._, syntax._
```
In all the example code using `just-fp` below, I assume that you've already imported `just-fp` at the top.

# Either

## Right-Biased `Either`
`Either` in Scala prior to 2.12 is not right-biased meaning that you have to call `Either.right` all the time if you want to use it with `for-comprehension`.

e.g.) Before 2.12
```scala
for {
  b <- methodReturningEither(a).right
  c <- anotherReturningEither(b).right
} yield c
```
If you use `just-fp`, it becomes
```scala
for {
  b <- methodReturningEither(a)
  c <- anotherReturningEither(b)
} yield c
```
Of course, you don't need to do it if you use Scala 2.12 or higher.

## Either Constructors
In normal ways, if you want to create `Left` or `Right`, you just use the `apply` methods of their companion objects (i.e. `Left()` `Right()`) A problem with this is that what these return is not `Either` but its data, `Left` or `Right`.

You also need to specify not only type parameter for `Left` but also the one for `Right` when creating `Right`.

e.g.) Without type parameters,
```scala
Right(1)
// Right[Nothing, Int] = Right(1)
``` 
You don't want to have `Nothing` there. So do it with type parameters,
```scala
Right[String, Int](1)
// Right[String, Int] = Right(1)
```
So it becomes unnecessarily verbose. Right should be inferred as the compiler knows it already yet to specify the left one, you have to put both left and right parameters.

`Left`, of course, has the same problem.

```scala
Left("error")
// Left[String, Nothing] = Left("error")
```
```scala
Left[String, Int]("error")
// Left[String, Int] = Left("error")
```

Now with `just-fp`, it's simpler. You can use use `left` and `right` constructors as extension methods to the actual data values with only missing type info specified.

e.g.)
```scala
1.right[String] // Now you only need to specify
                // the missing type info only
                // that is Left type parameter.
// Either[String, Int] = Right(1)
```
For `Left`,
```scala
"error".left[Int]
// Either[String, Int] = Left("error")
```

## `leftMap` and `leftFlatMap`
So if you Scala 2.12 or higher or `just-fp` with the older Scala, `Either` is right-biassed. Then what about the `Left` case? Can I ever use `Left` for something useful like transforming the `Left` value to something else?

For that, `just-fp` has added `leftMap` and `leftFlatMap` to `Either`.
e.g.)
```scala
for {
  b <- f1(a).leftMap(AppError.calculatioError)
  c <- f2(b).leftMap(AppError.fromComputeError)
} yield c
// f1 returns Either[String, Int]
// f2 returns Either[ComputeError, Int]
// The result is Either[AppError, Int]
```

# Option
## Option Constructors
Similar to `Either`, creating `Option` can expose its data instead of type.

e.g.) The following code returns `Some[Int]` not `Option[Int]`.
```scala
Some(1)
// Some[Int] = Some(1)
```

```scala
None
// None.type = None
// Also None is None not Option[A] 
```

With `just-fp`,
```scala
1.some
// Option[Int] = Some(1)

none[String]
// Option[String] = None
```

# Type-safe Equal
`==` in Scala is not type safe so the following code can never be `true` as their types are different but it has no compile-time error.

```scala
1 == "1" // always false, no compile-time error
```

`just-fp` has `Equal` typeclass with typeclass instances for value types (`Byte`, `Short`, `Int`, `Char`, `Long`, `Float` and `Double`) as well as `String`, `BigInt` and `BigDecimal`.

It also has `Equal` typeclass instances for `WriterT`, `Writer` and `EitherT`.

With `just-fp`,
```scala
// use triple equals from just-fp
1 === "1"   // compile-time error
1 === 1     // true
"a" === "a" // true
1 !== 1     // false
1 !== 2     // true
```

If it's a `case class` the `equals` of which can be used for equality check, `NatualEqual` can be used.

e.g.)
```scala
case class Foo(n: Int)
```
```scala
Foo(1) === Foo(1)
       ^
error: value === is not a member of Foo
```
This can be solved by `NatualEqual`.

```scala
case class Foo(n: Int)

object Foo {
  implicit val eqaul: Equal[Foo] = Equal.equalA[Foo]
}

Foo(1) === Foo(1)
// Boolean = true

Foo(1) === Foo(2)
// Boolean = false

Foo(1) !== Foo(1)
// Boolean = false

Foo(1) !== Foo(2)
// Boolean = true
```

# Semi-Group
// To be updated ...

# Monoid
// To be updated ...

# Functor
// To be updated ...

# Applicative
// To be updated ...

# Monad
// To be updated ...

# WriterT / Writer
// To be updated ...

# EitherT
// To be updated ...
