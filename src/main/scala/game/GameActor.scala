/*
 * Copyright (C) 2013 Tomas Shestakov. <https://github.com/Megaprog/LatencyGame>
 */

package game

import akka.actor.{Props, ActorRefFactory, ActorRef, Actor}
import messages.{SpawnChar, GameOver, GameTimeout, GameStart}
import org.slf4j.LoggerFactory
import scala.collection.mutable.ArrayBuffer
import scala.util.Random
import scala.concurrent.duration.FiniteDuration
import java.util.concurrent.TimeUnit

/**
 * User: Tomas
 * Date: 29.12.13
 * Time: 12:27
 */
class GameActor extends Actor {
  import GameActor.logger
  import context._

  var gameData = Option.empty[GameData]

  def receive: Actor.Receive = {
    case gameStart @ GameStart(timeout, candidates) =>
      logger.info(s"Game started with $candidates")

      gameData = Some(createGameData(sender, candidates))
      candidates foreach(_ ! gameStart)

      scheduleChar()
      scheduleTimeout(timeout)

    case spawnChar @ SpawnChar(char) =>
      gameData foreach { data =>
        data.players foreach(_ ! spawnChar)
      }
      scheduleChar()

    case GameTimeout => gameOver(None, GameOver.Reason.Timeout)

    case input: Character =>
      logger.info(s"$sender > $input")
  }

  class GameData(val manager: ActorRef, val players: Seq[ActorRef], var characters: Traversable[Char], val targetChar: Char)

  def createGameData(manager: ActorRef, candidates: Seq[ActorRef]) = {
    new GameData(
      manager,
      candidates,
      Random.shuffle((1 to 3).map(Character.forDigit(_, 10))),
      '3')
  }

  def scheduleChar() {
    gameData foreach { data =>
      if (!data.characters.nonEmpty) {
        system.scheduler.scheduleOnce(FiniteDuration(2 + Random.nextInt(3), TimeUnit.SECONDS), self, SpawnChar(data.characters.head))
        data.characters = data.characters.tail
      }
    }
  }

  def scheduleTimeout(timeout: FiniteDuration) {
    system.scheduler.scheduleOnce(timeout, self, GameTimeout)
  }

  def gameOver(winner: Option[ActorRef], reason: GameOver.Reason.Value) {
    gameData foreach { data =>
      val result = GameOver(data.players, winner, reason)
      data.players foreach(_ ! result)
      data.manager ! result
    }

    gameData = None
    context.stop(self)
  }
}

object GameActor {
  val logger = LoggerFactory.getLogger(classOf[GameActor])

  def factory(actorRefFactory: ActorRefFactory) = () => actorRefFactory.actorOf(Props[GameActor])
}
