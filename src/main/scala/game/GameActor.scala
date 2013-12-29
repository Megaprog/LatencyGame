/*
 * Copyright (C) 2013 Tomas Shestakov. <https://github.com/Megaprog/LatencyGame>
 */

package game

import akka.actor.{Props, ActorRefFactory, ActorRef, Actor}
import messages.GameStart
import org.slf4j.LoggerFactory

/**
 * User: Tomas
 * Date: 29.12.13
 * Time: 12:27
 */
class GameActor extends Actor {
  import GameActor.logger

  def receive: Actor.Receive = {
    case GameStart(players) =>
      logger.info(s"Game started with $players")
  }
}

object GameActor {
  val logger = LoggerFactory.getLogger(classOf[GameActor])

  def factory(actorRefFactory: ActorRefFactory) = () => actorRefFactory.actorOf(Props[GameActor])
}
