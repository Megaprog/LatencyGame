/*
 * Copyright (C) 2013 Tomas Shestakov. <https://github.com/Megaprog/LatencyGame>
 */

package game

import akka.actor.{ActorRef, Props, ActorRefFactory, Actor}
import messages._
import org.slf4j.LoggerFactory
import scala.concurrent.duration.FiniteDuration
import java.util.concurrent.TimeUnit
import messages.GameStart

/**
 * User: Tomas
 * Date: 29.12.13
 * Time: 12:21
 */
class ManagerActor(playersInGame: Int, timeout: FiniteDuration, logDelay: FiniteDuration, gameActorFactory: () => ActorRef) extends Actor {
  import context.dispatcher
  import ManagerActor.logger
  
  var pending = List.empty[ActorRef]

  var startedGames = 0
  var finishedGames = 0
  var playingGames = 0
  var totalGames = 0L
  var disconnects = 0
  var lastLogTime = 0L

  override def preStart() {
    lastLogTime = System.currentTimeMillis()
    scheduleLog()
  }

  def receive: Actor.Receive = {
    case GameRequest =>
      logger.debug(s"Game request from $sender")

      pending = sender :: pending
      if (pending.size >= playersInGame) {
        gameActorFactory() ! GameStart(timeout, pending)
        pending = List.empty
        startedGames += 1
        playingGames += 1
      }

    case result: GameOver =>
      logger.debug(result.toString)
      finishedGames += 1
      playingGames -= 1
      totalGames += 1

    case Disconnect(_) => disconnects += 1

    case DoLogs =>
      val currentTime = System.currentTimeMillis()
      val period = TimeUnit.MILLISECONDS.toSeconds(currentTime - lastLogTime)

      logger.info(s"total played $totalGames games and $disconnects disconnects, playing now $playingGames games, " +
        s"started ${startedGames/period} finished ${finishedGames/period} games per second")

      startedGames = 0
      finishedGames = 0
      lastLogTime = currentTime

      scheduleLog()
 }

  def scheduleLog() {
    context.system.scheduler.scheduleOnce(logDelay, self, DoLogs)
  }
}

object ManagerActor {
  val logger = LoggerFactory.getLogger(classOf[ManagerActor])

  def create(actorRefFactory: ActorRefFactory, playersInGame: Int, timeout: FiniteDuration, logDelay: FiniteDuration, gameActorFactory: () => ActorRef) =
    actorRefFactory.actorOf(Props(classOf[ManagerActor], playersInGame, timeout, logDelay, gameActorFactory).withDispatcher("akka.io.pinned-dispatcher"))
}