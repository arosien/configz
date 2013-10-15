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
 * val config = ConfigFactory.load // or some other constructor from com.typesafe.config.ConfigFactory
 *
 * // Config instances may be appended using Config.withFallback() semantics. (Config has a Monoid[Config])
 * val combinedConfig = config |+| ConfigFactory.parseFile(...)
 *
 * // Define some paths to values of a certain type.
 * val boolPath: Configz[Boolean] = "some.path.to.a.bool".path[Boolean]
 * val intPath:  Configz[Int]     = "some.path.to.an.int".path[Int]
 *
 * // Get the values at the path, which may fail with a com.typesafe.config.ConfigException.
 * val boolProp: Settings[Boolean] = config.get(boolPath)
 * val intProp:  Settings[Int]     = config.get(intPath)
 *
 * // Configz is an applicative functor, so you can combine them (using scalaz operators like <*> or |@|):
 * val boolIntConfig: Configz[(Boolean, Int)]  = (boolPath |@| intPath)(_ -> _)
 * val boolIntProp:   Settings[(Boolean, Int)] = config.get(boolIntConfig)
 *
 * // Configz paths can have custom validation:
 * val validatedIntPath = intPath.validate((_: Int) > 1000, "some.path.to.an.int must be > 1000")
 * }}}
 */
package object configz {
  import com.typesafe.config._
  import scalaz._
  import Scalaz._
  import Configz._

  type Settings[+A] = ValidationNel[ConfigException, A]

  /** Additional methods on [[com.typesafe.config.Config]]. */
  implicit class ConfigOps(config: Config) {
    def get[A](configz: Configz[A]): Settings[A] = configz.settings(config)
  }

  implicit val ConfigEqual: Equal[Config] = Equal.equalA

  /** The zero of a Config is the empty config, and two Config instances
   * are appended into a Config containing the first Config, then "falling back"
   * on the second according to the withFallback() method.
   */
  implicit val ConfigMonoid: Monoid[Config] = Monoid.instance[Config](_.withFallback(_), ConfigFactory.empty)

  /** Lift a String to a (typed) path into a config. */
  implicit class StringOps(value: String) {
    def path[A](implicit atPath: Configz[String => A]): Configz[A] = value.point[Configz] <*> atPath
  }
}