/*
 * Copyright (C) 2013 Tomas Shestakov. <https://github.com/Megaprog/LatencyGame>
 */

package game

import akka.actor.{ActorRef, Props, ActorRefFactory, Actor}
import messages.{GameStart, GameOver, GameRequest}
import org.slf4j.LoggerFactory
import scala.concurrent.duration.FiniteDuration
import java.util.concurrent.TimeUnit

/**
 * User: Tomas
 * Date: 29.12.13
 * Time: 12:21
 */
class ManagerActor(playersInGame: Int, timeout: Int, gameActorFactory: () => ActorRef) extends Actor {
  import ManagerActor.logger
  
  var pending = List.empty[ActorRef]

  def receive: Actor.Receive = {
    case GameRequest =>
      logger.debug(s"Game request from $sender")

      pending = sender :: pending
      if (pending.size >= playersInGame) {
        gameActorFactory() ! GameStart(FiniteDuration(timeout, TimeUnit.MINUTES), pending)
        pending = List.empty
      }

    case result: GameOver => logger.debug(result.toString)
  }
}

object ManagerActor {
  val logger = LoggerFactory.getLogger(classOf[ManagerActor])

  def create(actorRefFactory: ActorRefFactory, playersInGame: Int, timeout: Int, gameActorFactory: () => ActorRef) =
    actorRefFactory.actorOf(Props(classOf[ManagerActor], playersInGame, timeout, gameActorFactory).withDispatcher("akka.io.pinned-dispatcher"))
}