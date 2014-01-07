/*
 * Copyright (C) 2014 Tomas Shestakov. <https://github.com/Megaprog/LatencyGame>
 */

package messages

import java.net.InetSocketAddress
import akka.actor.ActorRef

/**
 * User: Tomas
 * Date: 07.01.14
 * Time: 23:04
 */
case class ConnectionInfo(remoteAddress: InetSocketAddress, localAddress: InetSocketAddress, connection: ActorRef, pipeline: ActorRef)
