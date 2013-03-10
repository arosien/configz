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
 * // Configz is an applicative functor, so you can combine them (using scalaz operators like <*> or |@|):
 * val boolIntConfig: Configz[(Boolean, Int)]  = (boolPath |@| intPath)(_ -> _)
 * val boolIntProp:   Settings[(Boolean, Int)] = config.get(boolIntConfig)
 *
 * // Configz paths can have custom validation using the >=> (Kleisli) operator:
 * val validatedIntPath = intPath >=> validate((_: Int) > 1000, "some.path.to.an.int must be > 1000")
 * }}}
 */
package object configz {
  import com.typesafe.config._
  import scalaz._
  import Scalaz._
  import Validation.Monad._
  import Configz._

  type Settings[A] = ValidationNEL[ConfigException, A]

  implicit val SettingsBind = implicitly[Bind[Settings]]

  /** Validate a Configz path.
   * @param f predicate function
   * @param message failure message if f returns false
   * @return validation function to be composed with Configz via >=> operator
   */
  def validate[A](f: A => Boolean, message: String): A => Settings[A] = prop =>
    if (f(prop)) prop.successNel else new ConfigException.Generic(message).failNel[A]

  /** Additional methods on [[com.typesafe.config.Config]]. */
  class ConfigOps(config: Config) {
    def get[A](configz: Kleisli[Settings, Config, A]): ValidationNEL[ConfigException, A] = configz(config)
  }

  implicit def configToConfigOps(config: Config): ConfigOps = new ConfigOps(config)

  /** Lift a String to a (typed) path into a config. */
  case class StringOps(value: String) {
    def path[A](implicit atPath: Configz[String => A]): Configz[A] = value.pure[Configz] <*> atPath
  }

  implicit def stringToOps(value: String): StringOps = StringOps(value)
}