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
import akka.io.TcpPipelineHandler.WithinActorContext
import java.net.InetSocketAddress
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
    IO(Tcp) ! Connect(new InetSocketAddress(host, port))
  }

  def receive: Actor.Receive = {
    case CommandFailed(_: Connect) =>
      log.info(s"connection failed $self")
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

      log.info(s"TcpPipelineHandler $pipeline")

      connection ! Register(pipeline)

      context become handling(init)

      log.info(s"intellect is $intellect")
      intellect.attach((msg: String) => pipeline ! init.Command(msg))
  }

  def handling(init: TcpPipelineHandler.Init[TcpPipelineHandler.WithinActorContext, String, String]): Actor.Receive = {
    case init.Event(data) =>
      log.info(s"received $data")
      intellect.receive(data)

    case _: ConnectionClosed =>
      log.info(s"disc from $sender")
      producerRef ! BotDisconnected
      context stop self

    case _ => log.info(s"some message from $sender")
  }
}

object BotActor {

  def factory(actorSystem: ActorRefFactory, host: String, port: Int, intellects: java.util.List[() => BotIntellect]) = (producerRef: ActorRef) =>
    actorSystem.actorOf(Props(classOf[BotActor], host, port, producerRef, intellects.get(Random.nextInt(intellects.size())).apply()))
}

