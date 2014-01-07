/*
 * Copyright (C) 2014 Tomas Shestakov. <https://github.com/Megaprog/LatencyGame>
 */

import akka.actor._
import messages.{BotConnected, BotDisconnected, AddBots, LogBots}
import org.slf4j.LoggerFactory
import scala.concurrent.duration.FiniteDuration

/**
 * User: Tomas
 * Date: 01.01.14
 * Time: 23:00
 */
class BotsProducerActor(maxBots: Int, creationDelay: FiniteDuration, botsPerStep: Int, logDelay: FiniteDuration,
                        botFactory: (ActorRef) => ActorRef) extends Actor {
  import context.dispatcher
  import BotsProducerActor.log

  var botsNumber = 0
  var connected = 0

  override def preStart() {
    scheduleBots()
    scheduleLog()
  }

  def scheduleBots() {
    context.system.scheduler.scheduleOnce(creationDelay, self, AddBots)
  }

  def scheduleLog() {
    context.system.scheduler.scheduleOnce(logDelay, self, LogBots)
  }

  def receive: Actor.Receive = {
    case AddBots =>
      (1 to math.min(maxBots - botsNumber, botsPerStep)) foreach { _ =>
        val bot = botFactory(self)
        log.debug(s"produce $bot")
        context watch bot
        botsNumber += 1
      }
      scheduleBots()

    case LogBots =>
      log.info("The number of bots is {} connected {}", botsNumber, connected)
      scheduleLog()

    case BotConnected =>
      log.debug(s"connected $sender")
      connected +=1

    case BotDisconnected =>
      log.debug(s"disconnected $sender")
      connected -= 1

    case Terminated(bot) =>
      log.debug(s"stopped $bot")
      botsNumber -= 1
  }
}

object BotsProducerActor {
  val log = LoggerFactory.getLogger(classOf[BotsProducerActor])

  def create(actorSystem: ActorRefFactory, maxBots: Int, creationDelay: FiniteDuration, botsPerStep: Int, logDelay: FiniteDuration, botFactory: (ActorRef) => ActorRef) =
    actorSystem.actorOf(Props(classOf[BotsProducerActor], maxBots, creationDelay, botsPerStep, logDelay, botFactory).withDispatcher("akka.io.pinned-dispatcher"))
}
