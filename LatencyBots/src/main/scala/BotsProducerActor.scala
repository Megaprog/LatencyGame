import akka.actor.{ActorRef, Props, ActorRefFactory, Actor}
import scala.concurrent.duration.FiniteDuration

/*
 * Copyright (C) 2014 Tomas Shestakov. <https://github.com/Megaprog/LatencyGame>
 */

/**
 * User: Tomas
 * Date: 01.01.14
 * Time: 23:00
 */
class BotsProducerActor(maxBots: Int, creationDelay: FiniteDuration, botsPerStep: Int, logDelay: FiniteDuration, botFactory: (ActorRef) => Runnable) extends Actor {

  def receive: Actor.Receive = {
    case _ =>
  }
}

object BotsProducerActor {

  def create(actorSystem: ActorRefFactory, maxBots: Int, creationDelay: FiniteDuration, botsPerStep: Int, logDelay: FiniteDuration, botFactory: (ActorRef) => Runnable) =
    actorSystem.actorOf(Props(classOf[BotsProducerActor], maxBots, creationDelay, botsPerStep, logDelay, botFactory).withDispatcher("akka.io.pinned-dispatcher"))
}
