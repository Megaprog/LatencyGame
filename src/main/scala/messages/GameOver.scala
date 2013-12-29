/*
 * Copyright (C) 2013 Tomas Shestakov. <https://github.com/Megaprog/LatencyGame>
 */

package messages

import akka.actor.ActorRef

/**
 * User: Tomas
 * Date: 29.12.13
 * Time: 17:09
 */
case class GameOver(players: Seq[ActorRef], winner: Option[ActorRef], reason: GameOver.Reason.Value) {

}

object GameOver {

  object Reason extends Enumeration {
    val Normal, FalseStart, TimeOut = Value
  }
}
