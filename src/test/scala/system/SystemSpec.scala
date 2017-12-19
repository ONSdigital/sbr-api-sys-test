package system

import scala.concurrent.ExecutionContext.Implicits.global

import play.api.libs.json.{JsNull, JsValue}

import util._

class SystemSpec extends TestUtils {

  private val UNIT_LIST = List("VAT", "PAYE", "LEU", "CH", "ENT")
  private val REFERENCE_PERIOD_FORMAT = "yyyyMM"
  private val DELIMITER = "-"
  private val EXPECTED_API_CONTENT_TYPE = "application/json"
  private val EXPECTED_DOCS_CONTENT_TYPE = "text/html; charset=utf-8"

  private val SBR_API = "sbr-api"
  private val SBR_CONTROL_API = "sbr-control-api"
  private val SBR_ADMIN_DATA = "sbr-admin-data"

  private val defaultYearMonth = yearMonthConversion(defaultPeriod, REFERENCE_PERIOD_FORMAT)


  // TODO - fix children and parents testing null values

  behavior of SBR_API

  /**
    * GET REQUESTS
    */
  "A unit id search" should "return the corresponding unit record" taggedAs(Api, Search, Enterprise) in {
    request.singleGETRequest(s"$sbrBaseUrl/v1/search?id=$enterpriseUnit2").map{ resp =>
      resp.json.as[Seq[JsValue]].nonEmpty
      (resp.json.as[Seq[JsValue]].head \ "unitType").as[String] shouldEqual "ENT"
      (resp.json.as[Seq[JsValue]].head \ "vars" \ "ent_postcode").as[String] shouldEqual expectedPostCode
      (resp.json.as[Seq[JsValue]].head \ "children").as[JsValue].toString contains UNIT_LIST
      (resp.json.as[Seq[JsValue]].head \ "childrenJson").as[Seq[JsValue]].nonEmpty
      resp.status shouldEqual OK
      resp.header("Content-Type") shouldEqual Some(EXPECTED_API_CONTENT_TYPE)
    }
  }

  "A unit and period" should "return the specific unit record of the specified period" taggedAs(Api, Search,
    PeriodSearch, Enterprise) in {
    request.singleGETRequest(s"$sbrBaseUrl/v1/periods/$defaultPeriod/search?id=$enterpriseUnit2").map{ resp =>
      resp.json.as[Seq[JsValue]].nonEmpty
      (resp.json.as[Seq[JsValue]].head \ "id").as[String].toLong shouldEqual enterpriseUnit2
      (resp.json.as[Seq[JsValue]].head \ "period").as[String] shouldEqual String.join(DELIMITER,
        defaultYearMonth.getYear.toString, "0" + defaultYearMonth.getMonthOfYear.toString)
      (resp.json.as[Seq[JsValue]].head \ "unitType").as[String] shouldEqual "ENT"
      (resp.json.as[Seq[JsValue]].head \ "vars" \ "ent_postcode").as[String] shouldEqual expectedPostCode
      (resp.json.as[Seq[JsValue]].head \ "children").as[JsValue].toString contains UNIT_LIST
      (resp.json.as[Seq[JsValue]].head \ "childrenJson").as[Seq[JsValue]].nonEmpty
      resp.status shouldEqual OK
      resp.header("Content-Type") shouldEqual Some(EXPECTED_API_CONTENT_TYPE)
    }
  }

  "A legal unit search" should "call the bi api to GET result with unit result" taggedAs(Api, Search, LegalUnit) in {
    request.singleGETRequest(s"$sbrBaseUrl/v1/leus/$legalUnit").map { resp =>
      (resp.json \ "parents" \ "ENT").as[String].toLong shouldEqual expectedENTParent1
      (resp.json \ "unitType").as[String] shouldEqual "LEU"
      resp.status shouldEqual OK
      resp.header("Content-Type") shouldEqual Some(EXPECTED_API_CONTENT_TYPE)
    }
  }

  "An enterprise unit search" should "call the sbr control api to GET result with unit result" taggedAs(Api, Search,
    Enterprise) in {
    request.singleGETRequest(s"$sbrBaseUrl/v1/ents/$enterpriseUnit1").map { resp =>
      (resp.json.as[JsValue] \ "id").as[String].toLong shouldEqual enterpriseUnit1
      // has some children matched to list
      (resp.json.as[JsValue] \ "children").as[JsValue].toString contains UNIT_LIST
      (resp.json.as[JsValue] \ "childrenJson").as[Seq[JsValue]].nonEmpty
      (resp.json.as[JsValue] \ "unitType").as[String] contains "ENT"
      resp.status shouldEqual OK
      resp.header("Content-Type") shouldEqual Some(EXPECTED_API_CONTENT_TYPE)
    }
  }

  "A VAT unit search" should "call the admin data to GET result with unit result" taggedAs(Api, Search, VAT) in {
    request.singleGETRequest(s"$sbrBaseUrl/v1/vats/$vatUnit").map { resp =>
      (resp.json \ "parents" \ "ENT").as[String].toLong shouldEqual expectedENTParent1
      (resp.json \ "parents" \ "LEU").as[String].toInt shouldEqual expectedLEUParent1
      Option((resp.json \ "children").as[JsValue]).getOrNull shouldEqual JsNull
      (resp.json \ "vars" \  "birthdate").as[String] shouldEqual expectedBirthDate
      resp.status shouldEqual OK
      resp.header("Content-Type") shouldEqual Some(EXPECTED_API_CONTENT_TYPE)
    }
  }

  "A Paye unit search" should "call the admin data to GET result with unit result" taggedAs(Api, Search, PAYE) in {
    request.singleGETRequest(s"$sbrBaseUrl/v1/payes/$payeUnit").map { resp =>
      (resp.json \ "parents" \ "ENT").as[String].toLong shouldEqual expectedENTParent1
      (resp.json \ "parents" \ "LEU").as[String].toInt shouldEqual expectedLEUParent1
      Option((resp.json \ "children").as[JsValue]).getOrNull shouldEqual JsNull
      (resp.json \ "vars" \  "tradstyle3").as[String].toInt shouldEqual expectedEmployerCat
      resp.status shouldEqual OK
      resp.header("Content-Type") shouldEqual Some(EXPECTED_API_CONTENT_TYPE)
    }
  }

  "A company house reference number (CRN) unit search" should "call the admin data to GET result with unit " +
    "result" taggedAs(Api, Search, CompanyHouse) in {
    request.singleGETRequest(s"$sbrBaseUrl/v1/crns/$chUnit").map { resp =>
      (resp.json \ "parents" \ "ENT").as[String].toLong shouldEqual expectedENTParent1
      (resp.json \ "parents" \ "LEU").as[String].toInt shouldEqual expectedLEUParent1
      Option((resp.json \ "children").as[JsValue]).getOrNull shouldEqual JsNull
      (resp.json \ "vars" \  "countryoforigin").as[String] shouldEqual "United Kingdom"
      resp.status shouldEqual OK
      resp.header("Content-Type") shouldEqual Some(EXPECTED_API_CONTENT_TYPE)
    }
  }


  "A legal unit and period search" should "call the bi api to GET result with unit result" taggedAs(Api, Search,
    PeriodSearch, LegalUnit) in {
    request.singleGETRequest(s"$sbrBaseUrl/v1/periods/$defaultPeriod/leus/$legalUnit").map { resp =>
      resp.status shouldEqual NOT_IMPLEMENTED
    }
  }

  "A enterprise unit and period search" should "call the sbr control api to GET result with unit result" taggedAs(Api,
    Search, PeriodSearch, Enterprise) in {
    request.singleGETRequest(s"$sbrBaseUrl/v1/periods/$defaultPeriod/ents/$enterpriseUnit1").map { resp =>
      (resp.json.as[JsValue] \ "id").as[String].toLong shouldEqual enterpriseUnit1
      // has some children matched to list
      (resp.json.as[JsValue] \ "children").as[JsValue].toString contains UNIT_LIST
      (resp.json.as[JsValue] \ "childrenJson").as[Seq[JsValue]].nonEmpty
      (resp.json.as[JsValue] \ "unitType").as[String] contains "ENT"
      (resp.json.as[JsValue] \ "period").as[String] contains String.join(DELIMITER, defaultYearMonth.getYear.toString,
        "0" + defaultYearMonth.getMonthOfYear.toString)
      resp.status shouldEqual OK
      resp.header("Content-Type") shouldEqual Some(EXPECTED_API_CONTENT_TYPE)
    }
  }

  "A VAT unit and period search" should "call the admin data to GET result with unit result" taggedAs(Api, Search,
    PeriodSearch, VAT) in {
    request.singleGETRequest(s"$sbrBaseUrl/v1/periods/$defaultPeriod/vats/$vatUnit").map { resp =>
      (resp.json \ "id").as[String].toLong shouldEqual vatUnit
      (resp.json \ "period").as[String] shouldEqual String.join(DELIMITER, defaultYearMonth.getYear.toString,
        "0" + defaultYearMonth.getMonthOfYear.toString)
      (resp.json \ "parents" \ "ENT").as[String].toLong shouldEqual expectedENTParent1
      (resp.json \ "parents" \ "LEU").as[String].toInt shouldEqual expectedLEUParent1
      Option((resp.json \ "children").as[JsValue]).getOrNull shouldEqual JsNull
      (resp.json \ "vars" \  "birthdate").as[String] shouldEqual expectedBirthDate
      resp.status shouldEqual OK
      resp.header("Content-Type") shouldEqual Some(EXPECTED_API_CONTENT_TYPE)
    }
  }

  "A Paye unit and period search" should "call the admin data to GET result with unit result" taggedAs(Api, Search, PeriodSearch, PAYE) in {
    request.singleGETRequest(s"$sbrBaseUrl/v1/periods/$defaultPeriod/payes/$payeUnit").map { resp =>
      (resp.json \ "id").as[String] shouldEqual payeUnit
      (resp.json \ "period").as[String] shouldEqual String.join(DELIMITER, defaultYearMonth.getYear.toString,
        "0" + defaultYearMonth.getMonthOfYear.toString)
      (resp.json \ "parents" \ "ENT").as[String].toLong shouldEqual expectedENTParent1
      (resp.json \ "parents" \ "LEU").as[String].toInt shouldEqual expectedLEUParent1
      Option((resp.json \ "children").as[JsValue]).getOrNull shouldEqual JsNull
      (resp.json \ "vars" \  "tradstyle3").as[String].toInt shouldEqual expectedEmployerCat
      resp.status shouldEqual OK
      resp.header("Content-Type") shouldEqual Some(EXPECTED_API_CONTENT_TYPE)
    }
  }

  "A company house reference number (CRN) unit and period search" should
    "call the admin data to GET result with unit result" taggedAs(Api, Search, PeriodSearch, CompanyHouse) in {
    request.singleGETRequest(s"$sbrBaseUrl/v1/periods/$defaultPeriod/crns/$chUnit").map { resp =>
      (resp.json \ "id").as[String] shouldEqual chUnit
      (resp.json \ "period").as[String] shouldEqual String.join(DELIMITER, defaultYearMonth.getYear.toString,
        "0" + defaultYearMonth.getMonthOfYear.toString)
      (resp.json \ "parents" \ "ENT").as[String].toLong shouldEqual expectedENTParent1
      (resp.json \ "parents" \ "LEU").as[String].toInt shouldEqual expectedLEUParent1
      Option((resp.json \ "children").as[JsValue]).getOrNull shouldEqual JsNull
      (resp.json \ "vars" \  "countryoforigin").as[String] shouldEqual "United Kingdom"
      resp.status shouldEqual OK
      resp.header("Content-Type") shouldEqual Some(EXPECTED_API_CONTENT_TYPE)
    }
  }

  "Version request" should "GET a version listing" taggedAs(Api, Util) in {
    request.singleGETRequest(s"$sbrBaseUrl/version").map { resp =>
      (resp.json \ "moduleName").as[String] shouldEqual "sbr-api"
      (resp.json \ "scalaVersion").as[String] startsWith "2."
      (resp.json \ "version").as[String] endsWith "-SNAPSHOT"
      //      (resp.json \ "apiVersion").as[String] endsWith "-SNAPSHOT"
      resp.status shouldEqual OK
      resp.header("Content-Type") shouldEqual Some(EXPECTED_API_CONTENT_TYPE)
    }
  }

  "Health request" should "GET a healthy status of the api" taggedAs(Api, Util) in {
    request.singleGETRequest(s"$sbrBaseUrl/health").map { resp =>
      (resp.json \ "Status").as[String] shouldEqual "Ok"
      resp.status shouldEqual OK
      resp.header("Content-Type") shouldEqual Some(EXPECTED_API_CONTENT_TYPE)
    }
  }

  "Hitting the swagger docs route" should "GET the swagger doc ui to test endpoints" taggedAs(Api, Util) in {
    request.singleGETRequest(s"$sbrBaseUrl/docs").map { resp =>
      resp.status shouldEqual OK
      resp.header("Content-Type") shouldEqual Some(EXPECTED_DOCS_CONTENT_TYPE)
    }
  }

  /**
    * POST REQUESTS
    */



  behavior of SBR_CONTROL_API

  /**
    * GET REQUESTS
     */

  "A full business name request" should "GET ONS with children links" taggedAs(Control, Search, Enterprise) in {
    request.singleGETRequest(s"$controlBaseUrl/v1/units/$enterpriseUnit1").map { resp =>
      resp.json.as[Seq[JsValue]].nonEmpty
      (resp.json.as[Seq[JsValue]].head \ "id").as[String].toLong shouldEqual enterpriseUnit1
      // has some children matched to list
      (resp.json.as[Seq[JsValue]].head \ "children").as[JsValue].toString contains UNIT_LIST
      resp.status shouldEqual OK
      resp.header("Content-Type") shouldEqual Some(EXPECTED_API_CONTENT_TYPE)
    }
  }

  "A full business name request with a period path parameter" should
    "GET ONS with children links of the given period" taggedAs(Control, Search, PeriodSearch, Enterprise) in {
    request.singleGETRequest(s"$controlBaseUrl/v1/periods/$defaultPeriod/units/$enterpriseUnit2").map { resp =>
      resp.json.as[Seq[JsValue]].nonEmpty
      (resp.json.as[Seq[JsValue]].head \ "id").as[String].toLong shouldEqual enterpriseUnit2
      // has some children matched to list
      (resp.json.as[Seq[JsValue]].head \ "children").as[JsValue].toString contains UNIT_LIST
      resp.status shouldEqual OK
      resp.header("Content-Type") shouldEqual Some(EXPECTED_API_CONTENT_TYPE)
    }
  }

  "A search on a unit with known type VAT" should "GET ONS VAT links record only" taggedAs(Control, Search,
    TypeSearch, VAT) in {
    request.singleGETRequest(s"$controlBaseUrl/v1/types/VAT/units/$vatUnit").map { resp =>
      (resp.json.as[JsValue] \ "id").as[String].toLong shouldEqual vatUnit
      // has some children matched to list
      (resp.json.as[JsValue] \ "parents" \ "ENT").as[String].toLong shouldEqual enterpriseUnit1
      (resp.json.as[JsValue] \ "parents" \ "LEU").as[String].toLong shouldEqual legalUnit
      resp.status shouldEqual OK
      resp.header("Content-Type") shouldEqual Some(EXPECTED_API_CONTENT_TYPE)
    }
  }

  "A search on all three criteria" should "GET ONS links record matching that satisfies the given parameters" taggedAs(
    Control, Search, PeriodSearch, TypeSearch, Enterprise) in {
    request.singleGETRequest(s"$controlBaseUrl/v1/periods/$defaultPeriod/types/ENT/units/$enterpriseUnit1").map {
      resp =>
      (resp.json.as[JsValue] \ "id").as[String].toLong shouldEqual enterpriseUnit1
      // has some children matched to list
      (resp.json.as[JsValue] \ "children").as[JsValue].toString contains UNIT_LIST
      resp.status shouldEqual OK
      resp.header("Content-Type") shouldEqual Some(EXPECTED_API_CONTENT_TYPE)
    }
  }

  "An enterprise search using ONS full name" should "GET ONS children links and enterprise details" taggedAs(Control,
    Search, PeriodSearch, Enterprise) in {
    request.singleGETRequest(s"$controlBaseUrl/v1/periods/$defaultPeriod/enterprises/$enterpriseUnit1").map { resp =>
      (resp.json \ "id").as[Long] shouldEqual enterpriseUnit1
      (resp.json \ "childrenJson").as[Seq[JsValue]].nonEmpty
      (resp.json \ "period").as[String] shouldEqual String.join(DELIMITER, defaultYearMonth.getYear.toString,
       "0" + defaultYearMonth.getMonthOfYear.toString)
      (resp.json \ "vars").as[JsValue]
      resp.status shouldEqual OK
      resp.header("Content-Type") shouldEqual Some(EXPECTED_API_CONTENT_TYPE)
    }
  }

  "Version controller request for sbr-control" should "GET a version listing" taggedAs(Control, Util) in {
    request.singleGETRequest(s"$controlBaseUrl/version").map { resp =>
      (resp.json \ "name").as[String] shouldEqual "sbr-control-api"
      (resp.json \ "scalaVersion").as[String] startsWith "2."
      (resp.json \ "version").as[String] endsWith "-SNAPSHOT"
//      (resp.json \ "apiVersion").as[String] endsWith "-SNAPSHOT"
      resp.status shouldEqual OK
      resp.header("Content-Type") shouldEqual Some(EXPECTED_API_CONTENT_TYPE)
    }
  }

  "Health controller request for sbr-control" should "GET a healthy status of the api" taggedAs(Control, Util) in {
    request.singleGETRequest(s"$controlBaseUrl/health").map { resp =>
      (resp.json \ "Status").as[String] shouldEqual "Ok"
      resp.status shouldEqual OK
      resp.header("Content-Type") shouldEqual Some(EXPECTED_API_CONTENT_TYPE)
    }
  }

  "Hitting the swagger docs route request for sbr-control" should "GET the swagger doc ui to test endpoints" taggedAs(
    Control, Util) in {
    request.singleGETRequest(s"$controlBaseUrl/docs").map { resp =>
      resp.status shouldEqual OK
      resp.header("Content-Type") shouldEqual Some(EXPECTED_DOCS_CONTENT_TYPE)
    }
  }

  /**
    * POST REQUESTS
    */


  behavior of SBR_ADMIN_DATA

  /**
    * GET REQUESTS
    */

  "Version controller request for sbr-admin-data" should "GET a version listing" taggedAs(`Admin-Data`, Util) in {
    request.singleGETRequest(s"$adminDataBaseUrl/version").map { resp =>
      (resp.json \ "name").as[String] startsWith "sbr-admin-data"
      (resp.json \ "scalaVersion").as[String] startsWith "2."
      (resp.json \ "version").as[String] endsWith "-SNAPSHOT"
      //      (resp.json \ "apiVersion").as[String] endsWith "-SNAPSHOT"
      resp.status shouldEqual OK
      resp.header("Content-Type") shouldEqual Some(EXPECTED_API_CONTENT_TYPE)
    }
  }

  "Health controller request for sbr-admin-data" should "GET a healthy status of the api" taggedAs(`Admin-Data`,
    Util) in {
    request.singleGETRequest(s"$adminDataBaseUrl/health").map { resp =>
      (resp.json \ "Status").as[String] shouldEqual "Ok"
      resp.status shouldEqual OK
      resp.header("Content-Type") shouldEqual Some(EXPECTED_API_CONTENT_TYPE)
    }
  }

  "Hitting the swagger docs route request for sbr-admin-data" should "GET the swagger doc ui to test " +
    "endpoints" taggedAs(`Admin-Data`, Util) in {
    request.singleGETRequest(s"$adminDataBaseUrl/docs").map { resp =>
      resp.status shouldEqual OK
      resp.header("Content-Type") shouldEqual Some(EXPECTED_DOCS_CONTENT_TYPE)
    }
  }


}
