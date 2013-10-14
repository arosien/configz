package net.rosien.configz

import org.specs2.ScalaCheck
import org.specs2.Specification
import org.specs2.scalaz.ScalazMatchers
import org.specs2.scalaz.ValidationMatchers

class ConfigzSpec extends Specification with ScalaCheck with ScalazMatchers {
  import collection.JavaConversions._
  import com.typesafe.config._
  import org.scalacheck._
  import org.specs2.matcher.Parameters
  import scalaz._
  import Scalaz._

  def is = "Configz should" ^
    "accumulate errors" ! configz().accumulateErrors ^
    "read settings"     ! configz().settings ^
    "validate settings" ! configz().validate ^
    "be a Monoid"       ! configz().configMonoid ^
    end

  implicit val ArbConfig: Arbitrary[Config] =
    Arbitrary {
      Gen.oneOf(
        ConfigFactory.empty,
        ConfigFactory.load,
        ConfigFactory.parseMap(Map("foo" -> 12, "bar" -> "baz")))
    }

  case class configz()  {
    import com.typesafe.config._
    import net.rosien.configz._
    import scalaz._
    import Scalaz._
    import scalaz.scalacheck.ScalazProperties._

    val config = ConfigFactory.load
    val boolProp = "configz.bool".path[Boolean]
    val intProp = "configz.int".path[Int]

    def accumulateErrors = {
      val missing = "configz.asdf".path[String]
      val wrongType = "configz.bool".path[Int]

      (missing tuple wrongType).settings(config) must beLike {
        case Failure(NonEmptyList(head, tail @ _*)) =>
          (head must beAnInstanceOf[ConfigException.Missing]) and
          (tail.head must beAnInstanceOf[ConfigException.WrongType])
      }
    }

    def settings = config.get(boolProp tuple intProp) must beSuccessful (true, 1234)

    def validate = {
      val isUnder1000 = config.get(intProp.validate((_: Int) < 1000, "configz.int must be < 1000"))

      isUnder1000 must beFailing
    }

    def configMonoid = monoid.laws[Config]
  }
}