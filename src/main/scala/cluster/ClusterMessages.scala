package cluster

/**
  * Created by Brian.Yip on 7/22/2016.
  */

final case class AddWorkers(workers: Int)

final case class WorkersResult(workers: Int)

final case class IllegalWorkers(message: String)

