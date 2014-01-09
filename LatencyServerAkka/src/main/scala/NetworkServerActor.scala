/*
 * Copyright (C) 2014 Tomas Shestakov. <https://github.com/Megaprog/LatencyGame>
 */

import akka.actor._
import akka.io._
import akka.io.IO
import akka.io.Tcp._
import akka.io.Tcp.CommandFailed
import akka.io.Tcp.Connected
import akka.io.Tcp.SO.{TcpNoDelay, KeepAlive}
import java.net.InetSocketAddress
import pipeline.{ClosablePipelineHandler, TelnetNegotiationCutter, LineFraming}
import akka.io.TcpPipelineHandler.{Init, WithinActorContext}

/**
 * User: Tomas
 * Date: 06.01.14
 * Time: 13:06
 */
class NetworkServerActor(port: Int, clientFactory: (ActorRefFactory, Init[WithinActorContext, String, String]) => ActorRef) extends Actor with ActorLogging {
  import context.system

  override def preStart() {
    IO(Tcp) ! Bind(self, new InetSocketAddress(port)/*, options = List(KeepAlive(on = true))*/)
  }

  def receive: Actor.Receive = {
    case CommandFailed(bind: Bind) =>
      log.error("cannot bind address {}:{}", bind.localAddress.getAddress, bind.localAddress.getPort)
      system shutdown()

    case Bound(_) => context become bound()
  }

  def bound(): Actor.Receive = {
    case connected @ Connected(remote, local) =>
//      val init = TcpPipelineHandler.withLogger(log,
//        new StringByteStringAdapter("utf-8") >>
//        new TelnetNegotiationCutter >>
//        new TcpReadWriteAdapter >>
//        new BackpressureBuffer(lowBytes = 100, highBytes = 1000, maxBytes = 1000000)
//      )
//
//      val connection = sender
//      val client = clientFactory(context, init)
//      val pipeline = context.actorOf(Props(classOf[ClosablePipelineHandler[_, _, _]], init, connection, client))
//
//      connection ! Register(pipeline)
//      pipeline ! connected //to inform client about connection

      //sender ! Register(context.actorOf(Props[DummyHandler]))
  }
}

object NetworkServerActor {

  def create(actorSystem: ActorRefFactory, port: Int, clientFactory: (ActorRefFactory, Init[WithinActorContext, String, String]) => ActorRef) =
    actorSystem.actorOf(Props(classOf[NetworkServerActor], port, clientFactory).withDispatcher("akka.io.pinned-dispatcher"))
}

class DummyHandler extends Actor {

  def receive: Actor.Receive = {
    case msg @ _ => println(msg)
  }
}
