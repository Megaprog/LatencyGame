/*
 * Copyright (C) 2013 Tomas Shestakov. <https://github.com/Megaprog/LatencyGame>
 */

package game

import akka.actor.{ActorRef, Props, ActorRefFactory, Actor}
import messages.{GameOver, GameRequest}
import org.slf4j.LoggerFactory

/**
 * User: Tomas
 * Date: 29.12.13
 * Time: 12:21
 */
class ManagerActor(players: Int, gameActorFactory: (Seq[ActorRef]) => ActorRef) extends Actor {
  import ManagerActor.logger

  def receive: Actor.Receive = {
    case request @ GameRequest =>
      logger.info(s"$request from $sender")
    case result @ GameOver => logger.info(result.toString())
  }
}

object ManagerActor {
  val logger = LoggerFactory.getLogger(classOf[ManagerActor])

  def create(actorRefFactory: ActorRefFactory, players: Int, gameActorFactory: (Seq[ActorRef]) => ActorRef) =
    actorRefFactory.actorOf(Props(classOf[ManagerActor], players, gameActorFactory))
}