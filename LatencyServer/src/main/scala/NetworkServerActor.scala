/*
 * Copyright (C) 2014 Tomas Shestakov. <https://github.com/Megaprog/LatencyGame>
 */

import akka.actor._
import akka.io._
import akka.io.IO
import akka.io.Tcp._
import akka.io.Tcp.CommandFailed
import akka.io.Tcp.Connected
import java.net.InetSocketAddress
import messages.ConnectionInfo
import pipeline.{TelnetNegotiationCutter, LineFraming}
import akka.io.TcpPipelineHandler.{Init, WithinActorContext}

/**
 * User: Tomas
 * Date: 06.01.14
 * Time: 13:06
 */
class NetworkServerActor(port: Int, clientFactory: (ActorRefFactory, Init[WithinActorContext, String, String]) => ActorRef) extends Actor with ActorLogging {
  import context.system

  override def preStart() {
    IO(Tcp) ! Bind(self, new InetSocketAddress(port))
  }

  def receive: Actor.Receive = {
    case CommandFailed(bind: Bind) =>
      log.error(s"cannot bind address ${bind.localAddress.getAddress}:${bind.localAddress.getPort}")
      system shutdown()

    case Bound(_) => context become bound()
  }

  def bound(): Actor.Receive = {
    case Connected(remote, local) =>
      val init = TcpPipelineHandler.withLogger(log,
        new StringByteStringAdapter("utf-8") >>
        new LineFraming(512) >>
        new TelnetNegotiationCutter >>
        new TcpReadWriteAdapter >>
        new BackpressureBuffer(lowBytes = 100, highBytes = 1000, maxBytes = 1000000)
      )

      val connection = sender
      val client = clientFactory(context, init)
      val pipeline = context.actorOf(TcpPipelineHandler.props(init, connection, client))

      connection ! Register(pipeline/*, keepOpenOnPeerClosed = true*/)
      client ! ConnectionInfo(remote, local, connection, pipeline)
  }
}

object NetworkServerActor {

  def create(actorSystem: ActorRefFactory, port: Int, clientFactory: (ActorRefFactory, Init[WithinActorContext, String, String]) => ActorRef) =
    actorSystem.actorOf(Props(classOf[NetworkServerActor], port, clientFactory).withDispatcher("akka.io.pinned-dispatcher"))
}

