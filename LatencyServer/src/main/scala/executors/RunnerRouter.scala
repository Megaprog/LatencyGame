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
  val MaxThreads = 1000

  def apply(actorRefFactory: ActorRefFactory, nThreads: Int): ActorRef = nThreads match {
    case 1                             => actorRefFactory.actorOf(Props[RunnerActor].withDispatcher("akka.io.pinned-dispatcher"))
    case n if n > 1 && n <= MaxThreads => actorRefFactory.actorOf(Props[RunnerActor].withRouter(RoundRobinRouter(nrOfInstances = n)))
    case _                             => throw new IllegalArgumentException("Wrong threads number")
  }
}
