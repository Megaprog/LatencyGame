/*
 * Copyright (C) 2013 Tomas Shestakov. <https://github.com/Megaprog/LatencyGame>
 */

package executors

import akka.actor.{Props, ActorRefFactory, ActorRef}
import akka.routing.RoundRobinRouter

/**
 * User: Tomas
 * Date: 27.12.13
 * Time: 21:26
 */
object RunnerRouter {

  def apply(actorRefFactory: ActorRefFactory, nThreads: Int): ActorRef = actorRefFactory.actorOf(Props[RunnerActor].withRouter(
    RoundRobinRouter(nrOfInstances = nThreads)))
}
