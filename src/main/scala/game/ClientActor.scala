/*
 * Copyright (C) 2013 Tomas Shestakov. <https://github.com/Megaprog/LatencyGame>
 */

package game

import akka.actor.{Props, ActorRefFactory, ActorRef, Actor}
import io.netty.channel.socket.SocketChannel
import messages._
import scala.Some

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

    case GameStart(_, _) =>
      send("Противник найден.")
      send("Нажмите пробел, когда увидите цифру 3")

    case GameOver(_, winner, reason) =>
      if (winner == Some(self)) {
        if (reason == GameOver.Reason.Normal) {
          send("Вы нажали пробел первым и победили")
        }
        else if (reason == GameOver.Reason.FalseStart) {
          send("Ваш противник поспешил и вы выиграли")
        }
      }
      else {
        if (reason == GameOver.Reason.Normal) {
          send("Вы не успели и проиграли")
        }
        else if (reason == GameOver.Reason.FalseStart) {
          send("Вы поспешили и проиграли")
        }
        else if (reason == GameOver.Reason.TimeOut) {
          send("Время игры истекло, вы проиграли")
        }
      }

      disconnect()
  }

  def send(string: String) {
    channel.writeAndFlush(string + System.lineSeparator())
  }

  def disconnect() {
    channel.close()
  }
}

object ClientActor {

  def factory(actorRefFactory: ActorRefFactory, managerRef: ActorRef) =
    (ch: SocketChannel) => actorRefFactory.actorOf(Props(classOf[ClientActor], ch, managerRef))
}
