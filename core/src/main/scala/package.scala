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
  import com.typesafe.config._
  import scalaz._
  import Scalaz._
  import Configz._

  type Settings[A] = ValidationNEL[ConfigException, A]

  implicit val BooleanAtPath: Configz[String => Boolean] = atPath(config => path => config.getBoolean(path))
  implicit val IntAtPath:     Configz[String => Int]     = atPath(config => path => config.getInt(path))
  implicit val StringAtPath:  Configz[String => String]  = atPath(config => path => config.getString(path))
  implicit val ConfigAtPath:  Configz[String => Config]  = atPath(config => path => config.getConfig(path))
  // TODO: more AtPath instances

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