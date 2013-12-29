/*
 * Copyright (C) 2013 Tomas Shestakov. <https://github.com/Megaprog/LatencyGame>
 */

package game

import akka.actor.{Props, ActorRefFactory, ActorRef, Actor}

/**
 * User: Tomas
 * Date: 29.12.13
 * Time: 12:27
 */
class GameActor(players: Seq[ActorRef]) extends Actor {

  def receive: Actor.Receive = {
    case _ =>
  }
}

object GameActor {

  def factory(actorRefFactory: ActorRefFactory) =
    (players: Seq[ActorRef]) => actorRefFactory.actorOf(Props(classOf[GameActor], players))
}
