package cluster.http

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.StandardRoute
import akka.http.scaladsl.testkit.{ScalatestRouteTest, WSProbe}
import org.scalatest.{FlatSpec, Matchers}

/**
  * Created by Brian.Yip on 8/19/2016.
  */
class HttpServiceSpec extends FlatSpec with Matchers with ScalatestRouteTest with HttpService {

  override def workersExchangeRoute(nodeId: String): StandardRoute = {
    complete(nodeId)
  }

  "HttpRouter" should "respond to the default route" in {
    Get() ~> route ~> check {
      responseAs[String] shouldBe HttpService.greeting
    }
  }

  it should "send an echo reply back" in {
    val wsClient = WSProbe()

    WS("/ws/echo", wsClient.flow) ~> route ~> check {
      isWebSocketUpgrade shouldEqual true

      wsClient.sendMessage("FooBar")
      wsClient.expectMessage("ECHO: FooBar")
    }
  }

  it should "reply with the parameter provided" in {
    Get("/ws/workers-exchange?nodeId=1") ~> route ~> check {
      responseAs[String] shouldBe "1"
    }
  }
}
