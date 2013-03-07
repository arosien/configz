Oddly enough, a Scala wrapper for Typesafe's (pure-Java) Config library.

# Usage

```scala
import com.typesafe.config._
import net.rosien.configz._
import scalaz._
import Scalaz._

val config = ConfigFactory.load // or some other constructor from com.typesafe.config

// Define some paths to values of a certain type.
val boolPath: Configz[Boolean] = "some.path.to.a.bool".path[Boolean]
val intPath:  Configz[Int]     = "some.path.to.an.int".path[Int]

// Get the values at the path, which may fail with a com.typesafe.config.ConfigException.
val boolProp: ValidationNEL[ConfigException, Boolean] = config.get(boolPath)
val intProp:  ValidationNEL[ConfigException, Int]     = config.get(intPath)

// Configz is an applicative functor, so you can combine them (using scalaz operators):
val boolIntConfig: Configz[(Boolean, Int)] = (boolPath |@| intPath)(_ -> _)
val boolIntProp:   ValidationNEL[ConfigException, (Boolean, Int)] = config.get(boolIntConfig)
```