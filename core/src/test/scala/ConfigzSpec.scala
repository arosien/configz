package net.rosien.configz

import org.specs2.Specification
import org.specs2.scalaz.ValidationMatchers

class ConfigzSpec extends Specification with ValidationMatchers {
  def is = "Configz should" ^
    "accumulate errors"    ! configz().errors ^
    "read settings"        ! configz().settings ^
    "validate via kleisli" ! configz().validateKleisli ^
    end

  case class configz()  {
    import com.typesafe.config._
    import net.rosien.configz._
    import scalaz._
    import Scalaz._

    val config = ConfigFactory.load
    val boolProp = "configz.bool".path[Boolean]
    val intProp = "configz.int".path[Int]

    def errors = {
      val missing = "configz.asdf".path[String]
      val wrongType = "configz.bool".path[Int]

      (missing <|*|> wrongType).settings(config) must beFailing.like {
        case fails => fails.list must beLike {
          case (e1: ConfigException.Missing) :: (e2: ConfigException.WrongType) :: Nil => ok
        }
      }
    }

    def settings = config.get(boolProp <|*|> intProp) must beSuccessful(true -> 1234)

    def validateKleisli = {
      val validIntProp = validate((_: Int) < 1000, "configz.int must be < 1000")

      (config.get(intProp >=> validIntProp) must beFailing)
    }
  }
}