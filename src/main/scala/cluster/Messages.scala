package cluster

/**
  * Created by Brian.Yip on 7/27/2016.
  */

import generated.models.Worker

final case class SetWorkers(workers: Seq[Worker])

final case class WorkersResult(workers: Int)
