/*
 * Copyright (C) 2014 Tomas Shestakov. <https://github.com/Megaprog/LatencyGame>
 */

import akka.actor.{ActorRef, Props, ActorRefFactory, Actor}
import messages.{Connected, Disconnected, AddBots, LogBots}
import org.slf4j.LoggerFactory
import scala.concurrent.duration.FiniteDuration

/**
 * User: Tomas
 * Date: 01.01.14
 * Time: 23:00
 */
class BotsProducerActor(maxBots: Int, creationDelay: FiniteDuration, botsPerStep: Int, logDelay: FiniteDuration, botFactory: (ActorRef) => Runnable) extends Actor {
  import BotsProducerActor.logger
  import context._

  var botsNumber = 0
  var connected = 0

  override def preStart() {
    scheduleBots()
    scheduleLog()
  }

  def scheduleBots() {
    system.scheduler.scheduleOnce(creationDelay, self, AddBots)
  }

  def scheduleLog() {
    system.scheduler.scheduleOnce(logDelay, self, LogBots)
  }

  def receive: Actor.Receive = {
    case AddBots =>
      (1 to math.min(maxBots - botsNumber, botsPerStep)) foreach { _ =>
        botFactory(self).run()
        botsNumber += 1
      }
      scheduleBots()

    case LogBots =>
      logger.info("The number of bots is {} connected {}", botsNumber, connected)
      scheduleLog()

    case Connected =>
      connected +=1

    case Disconnected =>
      connected -= 1
      botsNumber -= 1
  }
}

object BotsProducerActor {
  val logger = LoggerFactory.getLogger(classOf[BotsProducerActor])

  def create(actorSystem: ActorRefFactory, maxBots: Int, creationDelay: FiniteDuration, botsPerStep: Int, logDelay: FiniteDuration, botFactory: (ActorRef) => Runnable) =
    actorSystem.actorOf(Props(classOf[BotsProducerActor], maxBots, creationDelay, botsPerStep, logDelay, botFactory).withDispatcher("akka.io.pinned-dispatcher"))
}
