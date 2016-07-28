package cluster.websocket

import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.{FlatSpec, Matchers}

/**
  * Created by Brian.Yip on 7/28/2016.
  */
class HttpMicroserviceSpec extends FlatSpec
  with Matchers
  with ScalatestRouteTest {

  "Service" should "serve the index page on /" in {
    //    Get("/ws-workers-exchange") ~> WebSocketMicroservice.workersFlow check {
    //      status shouldBe OK
    //    }
  }
}
