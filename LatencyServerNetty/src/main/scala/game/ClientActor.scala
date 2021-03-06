/*
 * Copyright (C) 2013 Tomas Shestakov. <https://github.com/Megaprog/LatencyGame>
 */

package game

import akka.actor.{Props, ActorRefFactory, ActorRef, Actor}
import io.netty.channel.socket.SocketChannel
import messages._
import scala.Some
import org.slf4j.LoggerFactory

/**
 * User: Tomas
 * Date: 29.12.13
 * Time: 12:59
 */
class ClientActor(channel: SocketChannel, managerRef: ActorRef) extends Actor {
  var gameRef = Option.empty[ActorRef]

  def receive: Actor.Receive = {
    case Connected =>
      send("Привет! Попробую найти тебе противника")
      managerRef ! GameRequest

    case Disconnected =>
      ClientActor.logger.debug("Disconnected")
      gameRef foreach(_ ! Disconnected)
      context.stop(self)

    case GameStart(_, _) =>
      send("Противник найден.")
      send("Нажмите пробел, когда увидите цифру 3")
      gameRef = Some(sender)

    case GameOver(_, winners, reason) =>
      if (winners.exists(_ == self)) {
        if (reason == GameOver.Reason.Normal) {
          send("Вы нажали пробел первым и победили")
        }
        else if (reason == GameOver.Reason.FalseStart) {
          send("Ваш противник поспешил и вы выиграли")
        }
        else if (reason == GameOver.Reason.Disconnect) {
          send("Ваш противник вышел из игры и вы выиграли")
        }
      }
      else {
        if (reason == GameOver.Reason.Normal) {
          send("Вы не успели и проиграли")
        }
        else if (reason == GameOver.Reason.FalseStart) {
          send("Вы поспешили и проиграли")
        }
        else if (reason == GameOver.Reason.Timeout) {
          send("Время игры истекло, вы проиграли")
        }
      }

      gameRef = None
      disconnect()

    case SpawnChar(char) => send(char.toString)

    case input: Character =>
      send("")
      gameRef foreach(_ ! input)
  }

  def send(string: String) {
    channel.writeAndFlush(string + "\r\n")
  }

  def disconnect() {
    channel.close()
  }
}

object ClientActor {
  val logger = LoggerFactory.getLogger(classOf[ClientActor])

  def factory(actorRefFactory: ActorRefFactory, managerRef: ActorRef) =
    (ch: SocketChannel) => actorRefFactory.actorOf(Props(classOf[ClientActor], ch, managerRef))
}
