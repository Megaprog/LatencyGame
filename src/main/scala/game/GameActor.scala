/*
 * Copyright (C) 2013 Tomas Shestakov. <https://github.com/Megaprog/LatencyGame>
 */

package game

import akka.actor._
import messages.{GameOver, GameTimeout}
import org.slf4j.LoggerFactory
import scala.util.Random
import scala.concurrent.duration.FiniteDuration
import java.util.concurrent.TimeUnit
import scala.Some
import messages.GameStart
import messages.SpawnChar

/**
 * User: Tomas
 * Date: 29.12.13
 * Time: 12:27
 */
class GameActor extends Actor {
  import GameActor._
  import context._

  var gameData = Option.empty[GameData]
  var timeoutTask = emptyCancellable

  def receive: Actor.Receive = {
    case gameStart @ GameStart(timeout, candidates) =>
      logger.info(s"Game started with $candidates")

      gameData = Some(createGameData(sender, candidates))
      candidates foreach(_ ! gameStart)

      scheduleChar()
      timeoutTask = scheduleTimeout(timeout)

    case spawnChar @ SpawnChar(char) =>
      logger.info(spawnChar.toString)

      gameData foreach { data =>
        if (char == data.targetChar) {
          data.hunterBegin = true
        }
        data.players foreach(_ ! spawnChar)
      }
      scheduleChar()

    case GameTimeout => gameOver(None, GameOver.Reason.Timeout)

    case ' ' =>
      logger.info(s"$sender --> 'space'")
      gameData foreach { data =>
        if (data.hunterBegin) {
          gameOver(Some(sender), GameOver.Reason.Normal)
        }
        else {
          gameOver(data.players.filter(_ != sender), GameOver.Reason.FalseStart)
        }
      }
  }

  class GameData(val manager: ActorRef, val players: Seq[ActorRef], var characters: Traversable[Char], val targetChar: Char = '3', var hunterBegin: Boolean = false) {
    override def toString: String = s"GameData(manager=$manager,players=$players,characters=$characters,targetChar=$targetChar,hunterBegin=$hunterBegin"
  }

  def createGameData(manager: ActorRef, candidates: Seq[ActorRef]) = {
    new GameData(
      manager,
      candidates,
      Random.shuffle((1 to 3).map(Character.forDigit(_, 10)))
    )
  }

  def scheduleChar() {
    gameData foreach { data =>
      if (data.characters.nonEmpty) {
        system.scheduler.scheduleOnce(FiniteDuration(2 + Random.nextInt(3), TimeUnit.SECONDS), self, SpawnChar(data.characters.head))
        data.characters = data.characters.tail
      }
    }
  }

  def scheduleTimeout(timeout: FiniteDuration) = {
    system.scheduler.scheduleOnce(timeout, self, GameTimeout)
  }

  def gameOver(winners: Traversable[ActorRef], reason: GameOver.Reason.Value) {
    gameData foreach { data =>
      val result = GameOver(data.players, winners, reason)
      data.players foreach(_ ! result)
      data.manager ! result
    }

    gameData = None
    timeoutTask.cancel()
    context.stop(self)
  }
}

object GameActor {
  val logger = LoggerFactory.getLogger(classOf[GameActor])

  val emptyCancellable = new Cancellable {
    def isCancelled: Boolean = true
    def cancel(): Boolean = false
  }

  def factory(actorRefFactory: ActorRefFactory) = () => actorRefFactory.actorOf(Props[GameActor])
}
