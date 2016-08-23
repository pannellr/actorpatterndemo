package cluster


/**
  * Created by Brian.Yip on 8/3/2016.
  */
sealed trait State

case object Idle extends State

case object Active extends State

case object AddingWorkers extends State

case object RemovingWorkers extends State

sealed trait Data

case object NoData extends Data
