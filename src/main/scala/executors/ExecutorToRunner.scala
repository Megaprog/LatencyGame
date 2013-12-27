/*
 * Copyright (C) 2013 Tomas Shestakov. <https://github.com/Megaprog/LatencyGame>
 */

package executors

import java.util.concurrent.Executor
import akka.actor.ActorRef

/**
 * User: Tomas
 * Date: 27.12.13
 * Time: 21:15
 */
case class ExecutorToRunner(runner: ActorRef) extends Executor {

  def execute(command: Runnable) {
    runner ! command
  }
}
