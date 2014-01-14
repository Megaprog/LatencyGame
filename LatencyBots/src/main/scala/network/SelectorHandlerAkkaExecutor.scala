/*
 * Copyright (C) 2014 Tomas Shestakov. <https://github.com/Megaprog/LatencyGame>
 */

package network

import java.util.concurrent.Executor
import akka.actor.ActorSystem

/**
 * User: Tomas
 * Date: 14.01.14
 * Time: 15:43
 */
class SelectorHandlerAkkaExecutor(actorSystem: ActorSystem, dispatcherId: String) extends SelectorHandler {

  protected override def createExecutor(): Executor = {
    actorSystem.dispatchers.lookup(dispatcherId)
  }
}
