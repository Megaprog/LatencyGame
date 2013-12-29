/*
 * Copyright (C) 2013 Tomas Shestakov. <https://github.com/Megaprog/LatencyGame>
 */

package game

import akka.actor.{Props, ActorRefFactory, ActorRef, Actor}
import messages.{GameOver, GameTimeout, GameStart}
import org.slf4j.LoggerFactory

/**
 * User: Tomas
 * Date: 29.12.13
 * Time: 12:27
 */
class GameActor extends Actor {
  import GameActor.logger

  var players = Seq.empty[ActorRef]
  var manager = Option.empty[ActorRef]

  def receive: Actor.Receive = {
    case GameStart(timeout, candidates) =>
      logger.info(s"Game started with $candidates")

      players = candidates
      manager = Some(sender)

      context.system.scheduler.scheduleOnce(timeout, self, GameTimeout)

    case GameTimeout => gameOver(None, GameOver.Reason.TimeOut)
  }

  def gameOver(winner: Option[ActorRef], reason: GameOver.Reason.Value) {
    val result = GameOver(players, winner, reason)
    players foreach(_ ! result)
    manager foreach(_ ! result)

    context.stop(self)
  }
}

object GameActor {
  val logger = LoggerFactory.getLogger(classOf[GameActor])

  def factory(actorRefFactory: ActorRefFactory) = () => actorRefFactory.actorOf(Props[GameActor])
}
