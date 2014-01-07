/*
 * Copyright (C) 2014 Tomas Shestakov. <https://github.com/Megaprog/LatencyGame>
 */

package executors

import java.util.concurrent.Executor
import akka.actor.{Props, ActorRefFactory, ActorRef}

/**
 * User: Tomas
 * Date: 27.12.13
 * Time: 21:15
 */
class ExecutorToRunner(val runner: ActorRef) extends Executor {

  def this(actorRefFactory: ActorRefFactory) = this(actorRefFactory.actorOf(Props[RunnerActor].withDispatcher("akka.io.pinned-dispatcher")))
  def this(actorRefFactory: ActorRefFactory, nThreads: Int) = this(RunnerRouter(actorRefFactory, nThreads))

  def execute(command: Runnable) {
    runner ! command
  }
}
