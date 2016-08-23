package cluster

import akka.actor.{ActorLogging, ActorRef, FSM}
import generated.models.{StartAddingWorkers, StopAddingWorkers}


/**
  * Created by Brian.Yip on 8/3/2016.
  */
// FSM uses state and data. In short, this actor will only respond to certain messages based on its state
// while making additional checks to the data it is passed.
class MessageSimulator(parentRef: ActorRef) extends FSM[State, Data] with ActorLogging {
  startWith(Idle, NoData)

  when(Idle) {
    case Event(StartAddingWorkers, noData) =>
      log.info("Adding workers!")
      goto(AddingWorkers) using noData replying "FooBar!"
  }

  when(AddingWorkers) {
    case Event(StopAddingWorkers, noData) =>
      sender ! "I am already adding workers!"
      stay
  }

}
