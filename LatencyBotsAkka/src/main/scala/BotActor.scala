/*
 * Copyright (C) 2014 Tomas Shestakov. <https://github.com/Megaprog/LatencyGame>
 */

import akka.actor._
import akka.io._
import akka.io.IO
import akka.io.Tcp._
import akka.io.Tcp.CommandFailed
import akka.io.Tcp.Connect
import akka.io.Tcp.Connected
import akka.io.Tcp.Register
import akka.io.Tcp.SO.KeepAlive
import akka.io.Tcp.SO.TcpNoDelay
import java.net.InetSocketAddress
import akka.io.TcpPipelineHandler.{Init, WithinActorContext}
import messages.{BotDisconnected, BotConnected}
import pipeline.{TelnetNegotiationCutter, LineFraming}
import scala.util.Random

/**
 * User: Tomas
 * Date: 06.01.14
 * Time: 13:06
 */
class BotActor(host: String, port: Int, producerRef: ActorRef, intellect: BotIntellect) extends Actor with ActorLogging {
  import context.system

  override def preStart() {
    IO(Tcp) ! Connect(new InetSocketAddress(host, port)/*, options = List(KeepAlive(on = true))*/ /*, timeout = Some(FiniteDuration(3, TimeUnit.SECONDS))*/)
  }

  def receive: Actor.Receive = {
    case CommandFailed(_: Connect) =>
      log.debug("connection failed {}", self)
      context stop self

    case Connected(_, _) =>
      producerRef ! BotConnected

      val init = TcpPipelineHandler.withLogger(log,
        new StringByteStringAdapter("utf-8") >>
        new LineFraming(512) >>
        new TelnetNegotiationCutter >>
        new TcpReadWriteAdapter >>
        new BackpressureBuffer(lowBytes = 100, highBytes = 1000, maxBytes = 1000000)
      )

      val connection = sender
      val pipeline = context.actorOf(TcpPipelineHandler.props(init, connection, self))

      connection ! Register(pipeline)

      context become handling(init)

      log.debug("intellect is {}", intellect)
      intellect.attach((msg: String) => pipeline ! init.Command(msg))
  }

  def handling(init: Init[WithinActorContext, String, String]): Actor.Receive = {
    case init.Event(data) =>
      log.debug("received {}", data)
      intellect.receive(data)

    case _: ConnectionClosed =>
      log.debug("disc from {}", sender)
      producerRef ! BotDisconnected
      context stop self
  }
}

object BotActor {

  def factory(actorSystem: ActorRefFactory, host: String, port: Int, intellects: java.util.List[() => BotIntellect]) = (producerRef: ActorRef) =>
    actorSystem.actorOf(Props(classOf[BotActor], host, port, producerRef, intellects.get(Random.nextInt(intellects.size())).apply()))
}

