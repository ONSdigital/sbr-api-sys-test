package system

import scala.concurrent.ExecutionContext.Implicits.global
import play.api.Application
import play.api.http.Status
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsNull, JsValue}
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
  private val EXPECTED_CONTENT_TYPE = "application/json"

  private val application: Application = new GuiceApplicationBuilder().build
  private val ws: WSClient = application.injector.instanceOf(classOf[WSClient])
  private val request = new RequestGenerator(ws)
  private val config: Config = ConfigFactory.load()

  private val sbrBaseUrl = config.getString("api.sbr.base.url")
  private val controlBaseUrl = config.getString("api.sbr-control.base.url")
  private val adminDataBaseUrl = config.getString("api.sbr-admin-data.base.url")

  private val enterpriseUnit1 = config.getLong("data-item.enterprise.unit.1")
  private val enterpriseUnit2 = config.getLong("data-item.enterprise.unit.2")
  private val legalUnit = config.getInt("data-item.legal.unit")
  private val vatUnit = config.getLong("vat.unit")
  private val payeUnit = config.getString("paye.unit")
  private val chUnit = config.getString("data-item.companies.house.unit")
  private val defaultPeriod = config.getInt("data-item.yearmonth.period")
  private val expectedPostCode = config.getString("data-item.expected.postcode")
  private val expectedENTParent1 = config.getLong("data-item.expected.enterprise.parent.1")
  private val expectedLEUParent1 = config.getInt("data-item.expected.legal.unit.parent.1")
  private val expectedBirthDate = config.getInt("expected.birth.date")
  private val expectedTradingStyle = config.getInt("expected.trading.style")


  private def yearMonthConversion(period: Int) = YearMonth.parse(period.toString,
    DateTimeFormat.forPattern(REFERENCE_PERIOD_FORMAT))



  behavior of "sbr-api"

  /**
    * GET REQUESTS
    */
  "A unit number search" should "return the corresponding unit record" in {
    request.singleGETRequest(s"$sbrBaseUrl/v1/search?id=$enterpriseUnit2").map{ resp =>
      resp.json.as[Seq[JsValue]].nonEmpty
      (resp.json.as[Seq[JsValue]].head \ "unitType").as[String] shouldEqual "ENT"
      (resp.json.as[Seq[JsValue]].head \ "vars" \ "ent_postCode").as[String] shouldEqual expectedPostCode
      (resp.json.as[Seq[JsValue]].head \ "children").as[JsValue].toString contains UNIT_LIST
      (resp.json.as[Seq[JsValue]].head \ "childrenJson").as[Seq[JsValue]].nonEmpty
      resp.status shouldEqual OK
      resp.header("Content-Type") shouldEqual Some(EXPECTED_CONTENT_TYPE)
    }
  }

  "A unit and period" should "return the specific unit record of the specified period" in {
    val yearMonth = yearMonthConversion(defaultPeriod)
    request.singleGETRequest(s"$sbrBaseUrl/v1/search?id=$enterpriseUnit2").map{ resp =>
      resp.json.as[Seq[JsValue]].nonEmpty
      (resp.json.as[Seq[JsValue]].head \ "id").as[String] shouldEqual enterpriseUnit2
      (resp.json.as[Seq[JsValue]].head \ "period").as[String] shouldEqual String.join(DELIMITER, yearMonth.getYear.toString,
      "0" + yearMonth.getMonthOfYear.toString)
      (resp.json.as[Seq[JsValue]].head \ "unitType").as[String] shouldEqual "ENT"
      (resp.json.as[Seq[JsValue]].head \ "vars" \ "ent_postCode").as[String] shouldEqual "OK16 5XQ"
      (resp.json.as[Seq[JsValue]].head \ "children").as[JsValue].toString contains UNIT_LIST
      (resp.json.as[Seq[JsValue]].head \ "childrenJson").as[Seq[JsValue]].nonEmpty
      resp.status shouldEqual OK
      resp.header("Content-Type") shouldEqual Some(EXPECTED_CONTENT_TYPE)
    }
  }

  "A legal unit search" should "call the bi api to GET result with unit result" in {
    request.singleGETRequest(s"$sbrBaseUrl/v1/leus/$legalUnit").map { resp =>
      (resp.json \ "parent" \ "ENT").as[String].toLong shouldEqual expectedENTParent1
      (resp.json \ "unitType").as[String] shouldEqual "LEU"
      resp.status shouldEqual OK
      resp.header("Content-Type") shouldEqual Some(EXPECTED_CONTENT_TYPE)
    }
  }

  "A enterprise unit search" should "call the sbr control api to GET result with unit result" in {
    request.singleGETRequest(s"$sbrBaseUrl/v1/leus/$enterpriseUnit1").map { resp =>
      (resp.json.as[JsValue] \ "id").as[String].toLong shouldEqual enterpriseUnit1
      // has some children matched to list
      (resp.json.as[JsValue] \ "children").as[JsValue].toString contains UNIT_LIST
      (resp.json.as[JsValue] \ "childrenJson").as[Seq[JsValue]].nonEmpty
      (resp.json.as[JsValue] \ "unitType").as[String] contains "ENT"
      resp.status shouldEqual OK
      resp.header("Content-Type") shouldEqual Some(EXPECTED_CONTENT_TYPE)
    }
  }

  "A VAT unit search" should "call the admin data to GET result with unit result" in {
    request.singleGETRequest(s"$sbrBaseUrl/v1/vats/$vatUnit").map { resp =>
      (resp.json \ "parents" \ "ENT").as[String].toLong shouldEqual expectedENTParent1
      (resp.json \ "parents" \ "LEU").as[String].toInt shouldEqual expectedLEUParent1
      (resp.json \ "children").as[JsNull.type]
      (resp.json \ "vars" \  "birthdate").as[String].toInt shouldEqual expectedBirthDate
      resp.status shouldEqual OK
      resp.header("Content-Type") shouldEqual Some(EXPECTED_CONTENT_TYPE)
    }
  }

  "A Paye unit search" should "call the admin data to GET result with unit result" in {
    request.singleGETRequest(s"$sbrBaseUrl/v1/payes/$payeUnit").map { resp =>
      (resp.json \ "parents" \ "ENT").as[String].toLong shouldEqual expectedENTParent1
      (resp.json \ "parents" \ "LEU").as[String].toInt shouldEqual expectedLEUParent1
      (resp.json \ "children").as[JsNull.type]
      (resp.json \ "vars" \  "tradstyle3").as[String].toInt shouldEqual expectedTradingStyle
      resp.status shouldEqual OK
      resp.header("Content-Type") shouldEqual Some(EXPECTED_CONTENT_TYPE)
    }
  }

  "A company house reference number (CRN) unit search" should "call the admin data to GET result with unit result" in {
    request.singleGETRequest(s"$sbrBaseUrl/v1/crns/$chUnit").map { resp =>
      (resp.json \ "parents" \ "ENT").as[String].toLong shouldEqual expectedENTParent1
      (resp.json \ "parents" \ "LEU").as[String].toInt shouldEqual expectedLEUParent1
      (resp.json \ "children").as[JsNull.type]
      (resp.json \ "vars" \  "countryoforigin").as[String] shouldEqual "United Kingdom"
      resp.status shouldEqual OK
      resp.header("Content-Type") shouldEqual Some(EXPECTED_CONTENT_TYPE)
    }
  }


  "A legal unit and period search" should "call the bi api to GET result with unit result" in {
    request.singleGETRequest(s"$sbrBaseUrl/v1/periods/$defaultPeriod/leus/$legalUnit").map { resp =>
      resp.status shouldEqual NOT_IMPLEMENTED
    }
  }

  "A enterprise unit and period search" should "call the sbr control api to GET result with unit result" in {
    val yearMonth = yearMonthConversion(defaultPeriod)
    request.singleGETRequest(s"$sbrBaseUrl/v1/leus/$enterpriseUnit1").map { resp =>
      (resp.json.as[JsValue] \ "id").as[String].toLong shouldEqual enterpriseUnit1
      // has some children matched to list
      (resp.json.as[JsValue] \ "children").as[JsValue].toString contains UNIT_LIST
      (resp.json.as[JsValue] \ "childrenJson").as[Seq[JsValue]].nonEmpty
      (resp.json.as[JsValue] \ "unitType").as[String] contains "ENT"
      (resp.json.as[JsValue] \ "period").as[String] contains String.join(DELIMITER, yearMonth.getYear.toString,
        "0" + yearMonth.getMonthOfYear.toString)
      resp.status shouldEqual OK
      resp.header("Content-Type") shouldEqual Some(EXPECTED_CONTENT_TYPE)
    }
  }

  "A VAT unit and period search" should "call the admin data to GET result with unit result" in {
    request.singleGETRequest(s"$sbrBaseUrl/v1/periods/$defaultPeriod/vats/$vatUnit").map { resp =>
      (resp.json \ "id").as[String] shouldEqual vatUnit
      (resp.json \ "period").as[String] shouldEqual defaultPeriod
      (resp.json \ "parents" \ "ENT").as[String].toLong shouldEqual expectedENTParent1
      (resp.json \ "parents" \ "LEU").as[String].toInt shouldEqual expectedLEUParent1
      (resp.json \ "children").as[JsNull.type]
      (resp.json \ "vars" \  "birthdate").as[String].toInt shouldEqual expectedBirthDate
      resp.status shouldEqual OK
      resp.header("Content-Type") shouldEqual Some(EXPECTED_CONTENT_TYPE)
    }
  }

  "A Paye unit and period search" should "call the admin data to GET result with unit result" in {
    request.singleGETRequest(s"$sbrBaseUrl/v1/periods/$defaultPeriod/payes/$payeUnit").map { resp =>
      (resp.json \ "id").as[String] shouldEqual payeUnit
      (resp.json \ "period").as[String] shouldEqual defaultPeriod
      (resp.json \ "parents" \ "ENT").as[String].toLong shouldEqual expectedENTParent1
      (resp.json \ "parents" \ "LEU").as[String].toInt shouldEqual expectedLEUParent1
      (resp.json \ "children").as[JsNull.type]
      (resp.json \ "vars" \  "tradstyle3").as[String].toInt shouldEqual expectedTradingStyle
      resp.status shouldEqual OK
      resp.header("Content-Type") shouldEqual Some(EXPECTED_CONTENT_TYPE)
    }
  }

  "A company house reference number (CRN) unit and period search" should "call the admin data to GET result with unit result" in {
    request.singleGETRequest(s"$sbrBaseUrl/v1/periods/$defaultPeriod/crns/$chUnit").map { resp =>
      (resp.json \ "id").as[String] shouldEqual chUnit
      (resp.json \ "period").as[String] shouldEqual defaultPeriod
      (resp.json \ "parents" \ "ENT").as[String].toLong shouldEqual expectedENTParent1
      (resp.json \ "parents" \ "LEU").as[String].toInt shouldEqual expectedLEUParent1
      (resp.json \ "children").as[JsNull.type]
      (resp.json \ "vars" \  "countryoforigin").as[String] shouldEqual "United Kingdom"
      resp.status shouldEqual OK
      resp.header("Content-Type") shouldEqual Some(EXPECTED_CONTENT_TYPE)
    }
  }

  "Version request" should "GET a version listing" in {
    request.singleGETRequest(s"$sbrBaseUrl/version").map { resp =>
      (resp.json \ "moduleName").as[String] shouldEqual "sbr-api"
      (resp.json \ "scalaVersion").as[String] startsWith "2."
      (resp.json \ "version").as[String] endsWith "-SNAPSHOT"
      //      (resp.json \ "apiVersion").as[String] endsWith "-SNAPSHOT"
      resp.status shouldEqual OK
      resp.header("Content-Type") shouldEqual Some(EXPECTED_CONTENT_TYPE)
    }
  }

  "Health request" should "GET a healthy status of the api" in {
    request.singleGETRequest(s"$sbrBaseUrl/health").map { resp =>
      (resp.json \ "Status").as[String] shouldEqual "Ok"
      resp.status shouldEqual OK
      resp.header("Content-Type") shouldEqual Some(EXPECTED_CONTENT_TYPE)
    }
  }

  "Hitting the swagger docs route" should "GET the swagger doc ui to test endpoints" in {
    request.singleGETRequest(s"$sbrBaseUrl/docs").map { resp =>
      resp.status shouldEqual OK
      resp.header("Content-Type") shouldEqual Some(EXPECTED_CONTENT_TYPE)
    }
  }

  /**
    * POST REQUESTS
    */




  behavior of "sbr-control"

  /**
    * GET REQUESTS
     */

  "A full business name request of ONS unit details" should "GET ONS with children links" in {
    request.singleGETRequest(s"$controlBaseUrl/v1/units/$enterpriseUnit1").map { resp =>
      resp.json.as[Seq[JsValue]].nonEmpty
      (resp.json.as[Seq[JsValue]].head \ "id").as[String].toLong shouldEqual enterpriseUnit1
      // has some children matched to list
      (resp.json.as[Seq[JsValue]].head \ "children").as[JsValue].toString contains UNIT_LIST
      resp.status shouldEqual OK
      resp.header("Content-Type") shouldEqual Some(EXPECTED_CONTENT_TYPE)
    }
  }

  "A full business name request of ONS with period parameter" should "GET ONS with children links of the given period" in {
    request.singleGETRequest(s"$controlBaseUrl/v1/periods/$defaultPeriod/units/$enterpriseUnit1").map { resp =>
      val yearMonth = yearMonthConversion(defaultPeriod)
      resp.json.as[Seq[JsValue]].nonEmpty
      (resp.json.as[Seq[JsValue]].head \ "id").as[String].toLong shouldEqual enterpriseUnit1
      (resp.json.as[Seq[JsValue]].head \ "period").as[String] shouldEqual String.join(DELIMITER, yearMonth.getYear.toString,
        "0" + yearMonth.getMonthOfYear.toString)
      // has some children matched to list
      (resp.json.as[Seq[JsValue]].head \ "children").as[JsValue].toString contains UNIT_LIST
      resp.status shouldEqual OK
      resp.header("Content-Type") shouldEqual Some(EXPECTED_CONTENT_TYPE)
    }
  }

  "A search on a unit with known type" should "GET ONS record of the specified type only" in {
    request.singleGETRequest(s"$controlBaseUrl/v1/types/ENT/units/$enterpriseUnit1").map { resp =>
      resp.json.as[Seq[JsValue]].nonEmpty
      (resp.json.as[Seq[JsValue]].head \ "id").as[String].toLong shouldEqual enterpriseUnit1
      (resp.json.as[Seq[JsValue]].head \ "unitType").as[String] shouldEqual "ENT"
      // has some children matched to list
      (resp.json.as[Seq[JsValue]].head \ "children").as[JsValue].toString contains UNIT_LIST
      resp.status shouldEqual OK
      resp.header("Content-Type") shouldEqual Some(EXPECTED_CONTENT_TYPE)
    }
  }

  "A search on all three criteria" should "GET ONS record" in {
    request.singleGETRequest(s"$controlBaseUrl/v1/periods/$defaultPeriod/types/ENT/units/$enterpriseUnit1").map { resp =>
      resp.json.as[Seq[JsValue]].nonEmpty
      (resp.json.as[Seq[JsValue]].head \ "id").as[String].toLong shouldEqual enterpriseUnit1
      // has some children matched to list
      (resp.json.as[Seq[JsValue]].head \ "children").as[JsValue].toString contains UNIT_LIST
      resp.status shouldEqual OK
      resp.header("Content-Type") shouldEqual Some(EXPECTED_CONTENT_TYPE)
    }
  }

  "An enterprise search using ONS full name" should "GET ONS children links and enterprise details" in {
    val yearMonth = yearMonthConversion(defaultPeriod)
    request.singleGETRequest(s"$controlBaseUrl/v1/periods/$defaultPeriod/enterprises/$enterpriseUnit1").map { resp =>
      (resp.json \ "id").as[Long] shouldEqual enterpriseUnit1
      (resp.json \ "childrenJson").as[Seq[JsValue]].nonEmpty
      (resp.json \ "period").as[String] shouldEqual String.join(DELIMITER, yearMonth.getYear.toString,
       "0" + yearMonth.getMonthOfYear.toString)
      (resp.json \ "vars").as[JsValue]
      resp.status shouldEqual OK
      resp.header("Content-Type") shouldEqual Some(EXPECTED_CONTENT_TYPE)
    }
  }

  "Version request" should "GET a version listing" in {
    request.singleGETRequest(s"$controlBaseUrl/version").map { resp =>
      (resp.json \ "name").as[String] shouldEqual "sbr-control-api"
      (resp.json \ "scalaVersion").as[String] startsWith "2."
      (resp.json \ "version").as[String] endsWith "-SNAPSHOT"
//      (resp.json \ "apiVersion").as[String] endsWith "-SNAPSHOT"
      resp.status shouldEqual OK
      resp.header("Content-Type") shouldEqual Some(EXPECTED_CONTENT_TYPE)
    }
  }

  "Health request" should "GET a healthy status of the api" in {
    request.singleGETRequest(s"$controlBaseUrl/health").map { resp =>
      (resp.json \ "Status").as[String] shouldEqual "Ok"
      resp.status shouldEqual OK
      resp.header("Content-Type") shouldEqual Some(EXPECTED_CONTENT_TYPE)
    }
  }

  "Hitting the swagger docs route" should "GET the swagger doc ui to test endpoints" in {
    request.singleGETRequest(s"$controlBaseUrl/docs").map { resp =>
      resp.status shouldEqual OK
      resp.header("Content-Type") shouldEqual Some(EXPECTED_CONTENT_TYPE)
    }
  }

  /**
    * POST REQUESTS
    */


  behavior of "sbr-admin-data"

  /**
    * GET REQUESTS
    */

  "Version request" should "GET a version listing" in {
    request.singleGETRequest(s"$adminDataBaseUrl/version").map { resp =>
      (resp.json \ "name").as[String] shouldEqual "sbr-admin-data"
      (resp.json \ "scalaVersion").as[String] startsWith "2."
      (resp.json \ "version").as[String] endsWith "-SNAPSHOT"
      //      (resp.json \ "apiVersion").as[String] endsWith "-SNAPSHOT"
      resp.status shouldEqual OK
      resp.header("Content-Type") shouldEqual Some(EXPECTED_CONTENT_TYPE)
    }
  }

  "Health request" should "GET a healthy status of the api" in {
    request.singleGETRequest(s"$adminDataBaseUrl/health").map { resp =>
      (resp.json \ "Status").as[String] shouldEqual "Ok"
      resp.status shouldEqual OK
      resp.header("Content-Type") shouldEqual Some(EXPECTED_CONTENT_TYPE)
    }
  }

  "Hitting the swagger docs route" should "GET the swagger doc ui to test endpoints" in {
    request.singleGETRequest(s"$adminDataBaseUrl/docs").map { resp =>
      resp.status shouldEqual OK
      resp.header("Content-Type") shouldEqual Some(EXPECTED_CONTENT_TYPE)
    }
  }


}
