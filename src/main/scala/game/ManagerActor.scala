/*
 * Copyright (C) 2013 Tomas Shestakov. <https://github.com/Megaprog/LatencyGame>
 */

package game

import akka.actor.{ActorRef, Props, ActorRefFactory, Actor}

/**
 * User: Tomas
 * Date: 29.12.13
 * Time: 12:21
 */
class ManagerActor(players: Int, gameActorFactory: (Seq[ActorRef]) => ActorRef) extends Actor {

  def receive: Actor.Receive = {
    case _ =>
  }
}

object ManagerActor {

  def create(actorRefFactory: ActorRefFactory, players: Int, gameActorFactory: (Seq[ActorRef]) => ActorRef) =
    actorRefFactory.actorOf(Props(classOf[ManagerActor], players, gameActorFactory))
}