/*
 * Copyright (C) 2013 Tomas Shestakov. <https://github.com/Megaprog/LatencyGame>
 */

package handlers

import io.netty.channel.{ChannelHandlerContext, SimpleChannelInboundHandler}
import org.slf4j.LoggerFactory
import akka.actor.ActorRef
import io.netty.channel.socket.SocketChannel
import messages.{Disconnected, Connected}

/**
 * User: Tomas
 * Date: 28.12.13
 * Time: 14:10
 */
class ClientHandler(clientActorRef: ActorRef) extends SimpleChannelInboundHandler[Character] {
  import ClientHandler.logger

  def messageReceived(ctx: ChannelHandlerContext, msg: Character) {
    clientActorRef ! msg
  }

  override def channelActive(ctx: ChannelHandlerContext) {
    clientActorRef ! Connected
  }

  override def channelInactive(ctx: ChannelHandlerContext) {
    clientActorRef ! Disconnected
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
    logger.error("Unexpected exception from downstream", cause)
    ctx.close()
  }
}

object ClientHandler {
  val logger = LoggerFactory.getLogger(classOf[ClientHandler])

  def factory(clientActorFactory: (SocketChannel) => ActorRef) = (ch: SocketChannel) => new ClientHandler(clientActorFactory(ch))
}