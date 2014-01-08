/*
 * Copyright (C) 2013 Tomas Shestakov. <https://github.com/Megaprog/LatencyGame>
 */

package game

import akka.actor._
import messages._
import akka.io.TcpPipelineHandler.{Init, WithinActorContext}
import akka.io.Tcp.{Close, Connected}
import messages.GameStart
import messages.SpawnChar
import scala.Some

/**
 * User: Tomas
 * Date: 29.12.13
 * Time: 12:59
 */
class ClientActor(init: Init[WithinActorContext, String, String], managerRef: ActorRef) extends Actor with ActorLogging {
  var pipeline: ActorRef = _
  var gameRefOption = Option.empty[ActorRef]

  def receive: Actor.Receive = {
    case Connected(_, _) =>
      pipeline = sender
      context watch pipeline
      send("Привет! Попробую найти тебе противника")
      managerRef ! GameRequest

    case Terminated(some) if some == pipeline =>
      log.debug("Disconnected")
      context stop self

    case GameStart(_, _) =>
      send("Противник найден.")
      send("Нажмите пробел, когда увидите цифру 3")
      gameRefOption = Some(sender)

    case GameOver(_, winners, reason) =>
      if (winners.exists(_ == self)) {
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
        else if (reason == GameOver.Reason.Timeout) {
          send("Время игры истекло, вы проиграли")
        }
      }

      gameRefOption = None
      disconnect()

    case SpawnChar(char) => send(char.toString)

    case init.Event(input) =>
      send("")
      gameRefOption foreach(gameRef => input.foreach(gameRef ! _))
  }

  def send(string: String) {
    pipeline ! init.Command(string + System.lineSeparator())
  }

  def disconnect() {
    pipeline ! Close
  }
}

object ClientActor {

  def factory(managerRef: ActorRef) = (actorRefFactory: ActorRefFactory, init: Init[WithinActorContext, String, String]) =>
    actorRefFactory.actorOf(Props(classOf[ClientActor], init, managerRef))
}
