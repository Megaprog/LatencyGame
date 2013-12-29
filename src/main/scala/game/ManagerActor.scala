/*
 * Copyright (C) 2013 Tomas Shestakov. <https://github.com/Megaprog/LatencyGame>
 */

package game

import akka.actor.{ActorRef, Props, ActorRefFactory, Actor}
import messages.{GameStart, GameOver, GameRequest}
import org.slf4j.LoggerFactory
import scala.concurrent.duration._

/**
 * User: Tomas
 * Date: 29.12.13
 * Time: 12:21
 */
class ManagerActor(playersInGame: Int, gameActorFactory: () => ActorRef) extends Actor {
  import ManagerActor.logger
  
  var pending = List.empty[ActorRef]

  def receive: Actor.Receive = {
    case GameRequest =>
      logger.info(s"Game request from $sender")

      pending = sender :: pending
      if (pending.size >= playersInGame) {
        gameActorFactory() ! GameStart(1.minutes, pending)
        pending = List.empty
      }

    case result: GameOver => logger.info(result.toString)
  }
}

object ManagerActor {
  val logger = LoggerFactory.getLogger(classOf[ManagerActor])

  def create(actorRefFactory: ActorRefFactory, playersInGame: Int, gameActorFactory: () => ActorRef) =
    actorRefFactory.actorOf(Props(classOf[ManagerActor], playersInGame, gameActorFactory).withDispatcher("akka.io.pinned-dispatcher"))
}