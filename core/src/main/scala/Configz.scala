package net.rosien.configz

import com.typesafe.config._
import scalaz._
import Scalaz._

/** Reads settings from a [[com.typesafe.config.Config]]. */
sealed trait Configz[A] {
  /** Read the settings from a config. */
  def settings(config: Config): Settings[A]
}

object Configz {
  implicit val ConfigzPure: Pure[Configz] = new Pure[Configz] {
    def pure[A](a: => A): Configz[A] = new Configz[A] {
      def settings(config: Config) =
        try a.success catch {
          case e: ConfigException => e.failNel
        }

      override def toString = try "Configz(Pure)[%s]".format(a.toString) catch { case e: ConfigException => e.getMessage }
    }
  }

  implicit val ConfigzApply: Apply[Configz] = new Apply[Configz] {
    def apply[A, B](f: Configz[A => B], a: Configz[A]) = new Configz[B] {
      def settings(config: Config) =
        try a.settings(config) <*> f.settings(config) catch {
          case e: ConfigException => e.failNel
        }

      override def toString = "Configz(Apply)[%s <*> %s]".format(a, f)
    }
  }

  implicit val ConfigzFunctor: Functor[Configz] = new Functor[Configz] {
    def fmap[A, B](r: Configz[A], f: A => B) = new Configz[B] {
      def settings(config: Config) = r.settings(config).map(f)
    }
  }

  class ValidatedConfigz[A](configz: Configz[A], reference: Config, paths: String*) extends Configz[A] {
    def settings(config: Config) =
      try {
        config.checkValid(reference, paths: _*)
        configz.settings(config)
      } catch {
        case e: ConfigException => e.failNel
      }
  }

  class ResolvedConfigz[A](configz: Configz[A]) extends Configz[A] {
    def settings(config: Config) =
      try config.resolve.get(configz) catch {
        case e: ConfigException => e.failNel
      }
  }

  /** Get a value at a path from a [[com.typesafe.config.Config]]. */
  def atPath[A](f: Config => String => A): Configz[String => A] = new Configz[String => A] {
    def settings(config: Config): Settings[String => A] = f(config).pure[Configz].settings(config)

    override def toString = "Configz(atPath)[%s]".format(f)
  }
}
