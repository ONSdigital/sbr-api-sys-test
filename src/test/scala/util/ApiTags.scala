package util

import org.scalatest.Tag

/**
  * Api
  * ----------------
  * Author: haqa
  * Date: 19 December 2017 - 12:30
  * Copyright (c) 2017  Office for National Statistics
  */

object Api extends Tag("uk.gov.ons.sbr.api")
object Control extends Tag("uk.gov.ons.sbr.control.api")
object `Admin-Data` extends Tag("uk.gov.ons.sbr.admin.data")

object Search extends Tag("uk.gov.ons.sbr.apis.search")
object PeriodSearch extends Tag("uk.gov.ons.sbr.apis.period.search")
object TypeSearch extends Tag("uk.gov.ons.sbr.apis.type.search")
object Util extends Tag("uk.gov.ons.sbr.apis.util")

object Enterprise extends Tag("uk.gov.ons.sbr.enterprise")
object LegalUnit extends Tag("uk.gov.ons.bi.legal.unit")
object PAYE extends Tag("uk.gov.ons.paye")
object VAT extends Tag("uk.gov.ons.vat")
object CompanyHouse extends Tag("uk.gov.ons.ch")

object CloudFoundry extends Tag("uk.gov.ons.deployment.system.test.cloudfoundry")
object Gateway extends Tag("uk.gov.ons.deployment.system.test.gateway")
