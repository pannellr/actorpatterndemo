package cluster

import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.{FlatSpec, Matchers}

/**
  * Created by Brian.Yip on 8/19/2016.
  */
class HttpRouterSpec extends FlatSpec with Matchers with ScalatestRouteTest with HttpService {

  "HttpRouter" should "respond to the default route" in {
    Get() ~> route ~> check {
      responseAs[String] shouldBe greeting
    }
  }

  it should "send an echo reply back" in {
    Get("/ws/echo") ~> route ~> check {
      responseAs[String] shouldBe echoMessage
    }
  }

  it should "reply with the parameter provided" in {
    Get("/ws/workers-exchange?nodeId=1") ~> route ~> check {
      responseAs[String] shouldBe "1"
    }
  }
}
