package net.rosien

/** Oddly enough, a Scala wrapper for Typesafe's (pure-Java) Config library.
 *
 * ==Usage==
 * {{{
 * import com.typesafe.config._
 * import net.rosien.configz._
 * import scalaz._
 * import Scalaz._
 *
 * val config = ConfigFactory.load // or some other constructor from com.typesafe.config
 *
 * // Define some paths to values of a certain type.
 * val boolPath: Configz[Boolean] = "some.path.to.a.bool".path[Boolean]
 * val intPath:  Configz[Int]     = "some.path.to.an.int".path[Int]
 *
 * // Get the values at the path, which may fail with a com.typesafe.config.ConfigException.
 * val boolProp: Settings[Boolean] = config.get(boolPath)
 * val intProp:  Settings[Int]     = config.get(intPath)
 *
 * // Configz is an applicative functor, so you can combine them:
 * val boolIntConfig: Configz[(Boolean, Int)]  = (boolPath |@| intPath)(_ -> _)
 * val boolIntProp:   Settings[(Boolean, Int)] = config.get(boolIntProp)
 * }}}
 */
package object configz {
  import collection.JavaConversions._
  import com.typesafe.config._
  import scalaz._
  import Scalaz._
  import Configz._

  type Settings[A] = ValidationNEL[ConfigException, A]

  implicit val BooleanAtPath:     Configz[String => Boolean]       = atPath(config => path => config.getBoolean(path))
  implicit val BooleanListAtPath: Configz[String => List[Boolean]] = atPath(config => path => config.getBooleanList(path).toList.map(Boolean.unbox))
  implicit val ConfigAtPath:      Configz[String => Config]        = atPath(config => path => config.getConfig(path))
  implicit val ConfigListAtPath:  Configz[String => List[Config]]  = atPath(config => path => config.getConfigList(path).toList)
  implicit val DoubleAtPath:      Configz[String => Double]        = atPath(config => path => config.getDouble(path))
  implicit val DoubleListAtPath:  Configz[String => List[Double]]  = atPath(config => path => config.getDoubleList(path).toList.map(Double.unbox))
  implicit val IntAtPath:         Configz[String => Int]           = atPath(config => path => config.getInt(path))
  implicit val IntListAtPath:     Configz[String => List[Int]]     = atPath(config => path => config.getIntList(path).toList.map(Int.unbox))
  implicit val LongAtPath:        Configz[String => Long]          = atPath(config => path => config.getLong(path))
  implicit val LongListAtPath:    Configz[String => List[Long]]    = atPath(config => path => config.getLongList(path).toList.map(Long.unbox))
  implicit val NumberAtPath:      Configz[String => Number]        = atPath(config => path => config.getNumber(path))
  implicit val NumberListAtPath:  Configz[String => List[Number]]  = atPath(config => path => config.getNumberList(path).toList)
  implicit val StringAtPath:      Configz[String => String]        = atPath(config => path => config.getString(path))
  implicit val StringListAtPath:  Configz[String => List[String]]  = atPath(config => path => config.getStringList(path).toList)

  /** Additional methods on [[com.typesafe.config.Config]]. */
  class ConfigOps(config: Config) {
    def get[A](configz: Configz[A]): Settings[A] = configz.settings(config)
  }

  implicit def configToConfigOps(config: Config): ConfigOps = new ConfigOps(config)

  /** Lift a String to a (typed) path into a config. */
  case class StringOps(value: String) {
    def path[A](implicit atPath: Configz[String => A]): Configz[A] = value.pure[Configz] <*> atPath
  }

  implicit def stringToOps(value: String): StringOps = StringOps(value)
}