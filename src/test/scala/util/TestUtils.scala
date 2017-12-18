package util

import com.github.nscala_time.time.Imports.YearMonth
import com.typesafe.config.{Config, ConfigFactory}
import org.joda.time.format.DateTimeFormat
import org.scalatest.{AsyncFlatSpec, Matchers}
import play.api.Application
import play.api.http.Status
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.WSClient

trait TestUtils extends AsyncFlatSpec with Matchers with Status {

  protected val application: Application = new GuiceApplicationBuilder().build
  protected val ws: WSClient = application.injector.instanceOf(classOf[WSClient])
  protected val request = new RequestGenerator(ws)
  protected val config: Config = ConfigFactory.load()

  protected val sbrBaseUrl = config.getString("api.sbr.base.url")
  protected val controlBaseUrl = config.getString("api.sbr-control.base.url")
  protected val adminDataBaseUrl = config.getString("api.sbr-admin-data.base.url")

  protected val enterpriseUnit1 = config.getLong("data-item.enterprise.unit.1")
  protected val enterpriseUnit2 = config.getLong("data-item.enterprise.unit.2")
  protected val legalUnit = config.getInt("data-item.legal.unit")
  protected val vatUnit = config.getLong("data-item.vat.unit")
  protected val payeUnit = config.getString("data-item.paye.unit")
  protected val chUnit = config.getString("data-item.companies.house.unit")
  protected val defaultPeriod = config.getInt("data-item.yearmonth.period")
  protected val expectedPostCode = config.getString("data-item.expected.postcode")
  protected val expectedENTParent1 = config.getLong("data-item.expected.enterprise.parent.1")
  protected val expectedLEUParent1 = config.getInt("data-item.expected.legal.unit.parent.1")
  protected val expectedBirthDate = config.getInt("data-item.expected.birth.date")
  protected val expectedTradingStyle = config.getInt("data-item.expected.trading.style")

  protected def yearMonthConversion(period: Int, format: String) = YearMonth.parse(period.toString,
    DateTimeFormat.forPattern(format))

}
