package system

import javax.inject.Inject

import play.api.libs.ws.WSClient
import org.scalatest._
import play.api.http.Status
import play.api.libs.json.JsValue
import com.typesafe.config.{Config, ConfigFactory}

import services.websocket.RequestGenerator

class SystemSpec @Inject() (ws: WSClient) extends FlatSpec with Matchers with Status {

  val init = new {
    val generator = new RequestGenerator(ws)
    val config: Config = ConfigFactory.load()
  }
//
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

  "A full business name request of ONS" should "return ONS record" in {
    val request = init.generator
    val baseUrl = init.config.getString("sbr-control-api.ons.enterprise.number.search")
    request.singleGETRequest(s"$baseUrl/v1/enterprises/").map{ resp =>
      resp.json.as[Seq[JsValue]].nonEmpty
      (resp.json.as[Seq[JsValue]].head \ "postCode").as[String] shouldEqual "NP10 8XG"
      resp.status shouldEqual OK
      resp.header("Content-Type") shouldEqual Some("application/json")
    }
  }
}
