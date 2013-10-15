package net.rosien.configz

import com.typesafe.config._
import scalaz._
import Scalaz._

/** Reads settings from a [[com.typesafe.config.Config]]. */
sealed trait Configz[A] { self =>
  /** Read the settings from a config. */
  def settings(config: Config): Settings[A]

  /** Validate the computed settings with a predicate.
   * @param f predicate function
   * @param message failure message if f returns false
   * @return a new instance that validates the settings
   */
  def validate(f: A => Boolean, message: String): Configz[A] =
    new Configz[A] {
      def settings(config: Config): Settings[A] =
        self.settings(config).ensure(NonEmptyList(new ConfigException.Generic(message)))(f)
    }
}

object Configz {
  implicit val ConfigzApplicative: Applicative[Configz] =
    new Applicative[Configz] {
      def point[A](a: => A): Configz[A] = new Configz[A] {
        def settings(config: Config) =
          try a.success catch {
            case e: ConfigException => e.failNel
          }
      }

      def ap[A, B](fa: => Configz[A])(f: => Configz[(A) => B]): Configz[B] =
        new Configz[B] {
          def settings(config: Config) =
            try fa.settings(config) <*> f.settings(config) catch {
              case e: ConfigException => e.failNel
            }
        }
  }

  implicit val ConfigzFunctor: Functor[Configz] =
    new Functor[Configz] {
      def map[A, B](fa: Configz[A])(f: A => B) =
        new Configz[B] {
          def settings(config: Config) = fa.settings(config) map f
        }
    }

  /** Get a value at a path from a [[com.typesafe.config.Config]]. */
  def atPath[A](f: Config => String => A): Configz[String => A] =
    new Configz[String => A] {
      def settings(config: Config): Settings[String => A] = f(config).point[Configz].settings(config)

      override def toString = "Configz(atPath)[%s]".format(f)
    }

  import collection.JavaConversions._

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
}
