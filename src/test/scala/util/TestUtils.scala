package util

import com.github.nscala_time.time.Imports.YearMonth
import com.typesafe.config.{Config, ConfigFactory}
import org.joda.time.format.DateTimeFormat
import org.scalatest.{AsyncFlatSpec, Matchers}
import play.api.Application
import play.api.http.Status
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsNull, JsValue}
import play.api.libs.ws.WSClient

trait TestUtils extends AsyncFlatSpec with Matchers with Status {

  protected val application: Application = new GuiceApplicationBuilder().build
  protected val ws: WSClient = application.injector.instanceOf(classOf[WSClient])
  protected val request = new RequestGenerator(ws)
  protected val config: Config = ConfigFactory.load()

  protected lazy val sbrBaseUrl: String = config.getString("api.sbr.base.url")
  protected lazy val controlBaseUrl: String = config.getString("api.sbr-control.base.url")
  protected lazy val adminDataBaseUrl: String = config.getString("api.sbr-admin-data.base.url")

  protected lazy val enterpriseUnit1: Long = config.getLong("data-item.enterprise.unit.1")
  protected lazy val enterpriseUnit2: Long = config.getLong("data-item.enterprise.unit.2")
  protected lazy val legalUnit: Long = config.getLong("data-item.legal.unit")
  protected lazy val vatUnit: Long = config.getLong("data-item.vat.unit")
  protected lazy val payeUnit: String = config.getString("data-item.paye.unit")
  protected lazy val chUnit: String = config.getString("data-item.companies.house.unit")
  protected lazy val defaultPeriod: Int = config.getInt("data-item.yearmonth.period")
  protected lazy val expectedPostCode: String = config.getString("data-item.expected.postcode")
  protected lazy val expectedENTParent1: Long = config.getLong("data-item.expected.enterprise.parent.1")
  protected lazy val expectedLEUParent1: Long = config.getLong("data-item.expected.legal.unit.parent.1")
  protected lazy val expectedBirthDate: String = config.getString("data-item.expected.birth.date")
  protected lazy val expectedEmployerCat: Int = config.getInt("data-item.expected.employer.cat")

  protected def yearMonthConversion(period: Int, format: String) = YearMonth.parse(period.toString,
    DateTimeFormat.forPattern(format))

  implicit class orElseNull(js: Option[JsValue]) {
    def getOrNull: JsValue = {
      js match {
        case Some(null) => JsNull
        case Some(j: JsValue) => j
        case None => sys.error("Unexpected match! Found None type -> failure")
      }
    }
  }

}
