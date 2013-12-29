/*
 * Copyright (C) 2013 Tomas Shestakov. <https://github.com/Megaprog/LatencyGame>
 */

package game

import akka.actor.{Props, ActorRefFactory, ActorRef, Actor}
import io.netty.channel.socket.SocketChannel
import messages.{GameRequest, Disconnected, Connected}

/**
 * User: Tomas
 * Date: 29.12.13
 * Time: 12:59
 */
class ClientActor(channel: SocketChannel, managerRef: ActorRef) extends Actor {

  def receive: Actor.Receive = {
    case Connected =>
      send("Привет! Попробую найти тебе противника")
      managerRef ! GameRequest

    case Disconnected => context.stop(self)
  }

  def send(string: String) {
    channel.writeAndFlush(string + System.lineSeparator())
  }
}

object ClientActor {

  def factory(actorRefFactory: ActorRefFactory, managerRef: ActorRef) =
    (ch: SocketChannel) => actorRefFactory.actorOf(Props(classOf[ClientActor], ch, managerRef))
}
