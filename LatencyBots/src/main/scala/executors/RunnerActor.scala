/*
 * Copyright (C) 2014 Tomas Shestakov. <https://github.com/Megaprog/LatencyGame>
 */

package executors

import akka.actor.Actor

/**
 * User: Tomas
 * Date: 27.12.13
 * Time: 21:08
 */
class RunnerActor extends Actor {

  def receive: Actor.Receive = {
    case runnable: Runnable =>

      println("RunnerActor run " + runnable)

      runnable.run()

      println("RunnerActor finished")
  }
}
