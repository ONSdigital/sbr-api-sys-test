package system

import scala.concurrent.ExecutionContext.Implicits.global

import play.api.Application
import play.api.http.Status
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.JsValue
import play.api.libs.ws.WSClient
import org.joda.time.format.DateTimeFormat
import org.scalatest._
import com.typesafe.config.{Config, ConfigFactory}

import util.RequestGenerator
import com.github.nscala_time.time.Imports.YearMonth

class SystemSpec extends AsyncFlatSpec with Matchers with Status {

  private val application: Application = new GuiceApplicationBuilder().build
  private val ws: WSClient = application.injector.instanceOf(classOf[WSClient])
  private val request = new RequestGenerator(ws)
  private val config: Config = ConfigFactory.load()

  private val unitList = List("VAT", "PAYE", "LEU", "CH", "ENT")
  private val REFERENCE_PERIOD_FORMAT = "yyyyMM"
  private val DELIMITER = "-"


  private def yearMonthConversion(period: Int)  = YearMonth.parse(period.toString, DateTimeFormat.forPattern(REFERENCE_PERIOD_FORMAT))

//  behavior of "sbr-api"
//
//  "A full business name request of ONS" should "return ONS record" in {
//    val request = init.generator
//    request.singleGETRequest("").map{ resp =>
//      resp.json.as[Seq[JsValue]].nonEmpty
//      (resp.json.as[Seq[JsValue]].head \ "postCode").as[String] shouldEqual "NP10 8XG"
//      resp.status shouldEqual OK
//      resp.header("Content-Type") shouldEqual Some("application/json")
//    }
//  }

  behavior of "sbr-control"

  "A full business name request of ONS unit details" should "return ONS with children links" in {
    val baseUrl = config.getString("sbr-control-api.base.url")
    val enterprise = config.getString("sbr-control-api.ons.enterprise.number")
    request.singleGETRequest(s"$baseUrl/v1/units/$enterprise").map { resp =>
      resp.json.as[Seq[JsValue]].nonEmpty
      (resp.json.as[Seq[JsValue]].head \ "id").as[String].toLong shouldEqual enterprise.toLong
      // has some children matched to list
      (resp.json.as[Seq[JsValue]].head \ "children").as[JsValue].toString contains unitList
      resp.status shouldEqual OK
      resp.header("Content-Type") shouldEqual Some("application/json")
    }
  }


  "An enterprise search using ONS full name" should "return ONS children links and enterprise details" in {
    val baseUrl = config.getString("sbr-control-api.base.url")
    val enterprise = config.getString("sbr-control-api.ons.enterprise.number")
    val period = config.getInt("sbr-control-api.yearmonth.period")
    val yearMonth = yearMonthConversion(period)
    request.singleGETRequest(s"$baseUrl/v1/periods/$period/enterprises/$enterprise").map { resp =>
      (resp.json \ "id").as[String].toLong shouldEqual enterprise.toLong
      (resp.json \ "childrenJson").as[Seq[JsValue]].nonEmpty
      (resp.json \ "period").as[String] shouldEqual String.join(DELIMITER, yearMonth.getYear.toString,
        yearMonth.getMonthOfYear.toString)
      (resp.json \ "vars").as[String]
      resp.status shouldEqual OK
      resp.header("Content-Type") shouldEqual Some("application/json")
    }
  }

}
