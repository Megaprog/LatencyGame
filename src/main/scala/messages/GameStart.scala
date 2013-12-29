/*
 * Copyright (C) 2013 Tomas Shestakov. <https://github.com/Megaprog/LatencyGame>
 */

package messages

import akka.actor.ActorRef

/**
 * User: Tomas
 * Date: 29.12.13
 * Time: 17:48
 */
case class GameStart(players: Seq[ActorRef])
