package system

import scala.concurrent.ExecutionContext.Implicits.global

import play.api.Application
import play.api.http.Status
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.JsValue
import play.api.libs.ws.WSClient
import org.joda.time.format.DateTimeFormat
import org.scalatest._
import com.github.nscala_time.time.Imports.YearMonth
import com.typesafe.config.{Config, ConfigFactory}

import util.RequestGenerator

class SystemSpec extends AsyncFlatSpec with Matchers with Status {

  private val UNIT_LIST = List("VAT", "PAYE", "LEU", "CH", "ENT")
  private val REFERENCE_PERIOD_FORMAT = "yyyyMM"
  private val DELIMITER = "-"

  private val application: Application = new GuiceApplicationBuilder().build
  private val ws: WSClient = application.injector.instanceOf(classOf[WSClient])
  private val request = new RequestGenerator(ws)
  private val config: Config = ConfigFactory.load()

  private val defaultPeriod = config.getInt("sbr-control-api.yearmonth.period")
  private val controlBaseUrl = config.getString("sbr-control-api.base.url")
  private val onsEnterpriseNum = config.getString("sbr-control-api.ons.enterprise.number")

  private def yearMonthConversion(period: Int) = YearMonth.parse(period.toString,
    DateTimeFormat.forPattern(REFERENCE_PERIOD_FORMAT))


  behavior of "sbr-api"

  /**
    * GET REQUESTS
    */

  "A full business name request of ONS" should "return ONS record" in {
    request.singleGETRequest("").map{ resp =>
      resp.json.as[Seq[JsValue]].nonEmpty
      (resp.json.as[Seq[JsValue]].head \ "postCode").as[String] shouldEqual "NP10 8XG"
      resp.status shouldEqual OK
      resp.header("Content-Type") shouldEqual Some("application/json")
    }
  }


  behavior of "sbr-control"

  /**
    * GET REQUESTS
     */

  "A full business name request of ONS unit details" should "GET ONS with children links" in {
    request.singleGETRequest(s"$controlBaseUrl/v1/units/$onsEnterpriseNum").map { resp =>
      resp.json.as[Seq[JsValue]].nonEmpty
      (resp.json.as[Seq[JsValue]].head \ "id").as[String].toLong shouldEqual onsEnterpriseNum.toLong
      // has some children matched to list
      (resp.json.as[Seq[JsValue]].head \ "children").as[JsValue].toString contains UNIT_LIST
      resp.status shouldEqual OK
      resp.header("Content-Type") shouldEqual Some("application/json")
    }
  }

  "A search on all three criterias" should "GET ONS record" in {
    request.singleGETRequest(s"$controlBaseUrl/v1/periods/$defaultPeriod/types/ENT/units/$onsEnterpriseNum").map { resp =>
      resp.json.as[Seq[JsValue]].nonEmpty
      (resp.json.as[Seq[JsValue]].head \ "id").as[String].toLong shouldEqual onsEnterpriseNum.toLong
      // has some children matched to list
      (resp.json.as[Seq[JsValue]].head \ "children").as[JsValue].toString contains UNIT_LIST
      resp.status shouldEqual OK
      resp.header("Content-Type") shouldEqual Some("application/json")
    }
  }

  "An enterprise search using ONS full name" should "GET ONS children links and enterprise details" in {
    val yearMonth = yearMonthConversion(defaultPeriod)
    request.singleGETRequest(s"$controlBaseUrl/v1/periods/$defaultPeriod/enterprises/$onsEnterpriseNum").map { resp =>
      (resp.json \ "id").as[Long] shouldEqual onsEnterpriseNum.toLong
      (resp.json \ "childrenJson").as[Seq[JsValue]].nonEmpty
      (resp.json \ "period").as[String] shouldEqual String.join(DELIMITER, yearMonth.getYear.toString,
       "0" + yearMonth.getMonthOfYear.toString)
      (resp.json \ "vars").as[JsValue]
      resp.status shouldEqual OK
      resp.header("Content-Type") shouldEqual Some("application/json")
    }
  }

  "Version request" should "GET a version listing" in {
    request.singleGETRequest(s"$controlBaseUrl/version").map { resp =>
      (resp.json \ "name").as[String] shouldEqual "sbr-control-api"
      (resp.json \ "scalaVersion").as[String] startsWith "2."
      (resp.json \ "version").as[String] endsWith "-SNAPSHOT"
//      (resp.json \ "apiVersion").as[String] endsWith "-SNAPSHOT"
      resp.status shouldEqual OK
      resp.header("Content-Type") shouldEqual Some("application/json")
    }
  }

  "Health request" should "GET a healthy status of the api" in {
    request.singleGETRequest(s"$controlBaseUrl/health").map { resp =>
      (resp.json \ "Status").as[String] shouldEqual "OK"
      resp.status shouldEqual OK
      resp.header("Content-Type") shouldEqual Some("application/json")
    }
  }

  "Hitting the swagger docs route" should "GET the swagger doc ui to test endpoints" in {
    request.singleGETRequest(s"$controlBaseUrl/docs").map { resp =>
      resp.status shouldEqual OK
      resp.header("Content-Type") shouldEqual Some("application/json")
    }
  }

  /**
    * POST REQUESTS
    */




}
