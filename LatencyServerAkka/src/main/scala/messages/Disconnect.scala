/*
 * Copyright (C) 2014 Tomas Shestakov. <https://github.com/Megaprog/LatencyGame>
 */

package messages

import akka.actor.ActorRef

/**
 * User: Tomas
 * Date: 08.01.14
 * Time: 19:36
 */
case class Disconnect(player: ActorRef)
