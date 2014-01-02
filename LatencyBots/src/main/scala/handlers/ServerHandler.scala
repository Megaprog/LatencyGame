/*
 * Copyright (C) 2014 Tomas Shestakov. <https://github.com/Megaprog/LatencyGame>
 */

package handlers

import io.netty.channel.{ChannelHandlerContext, SimpleChannelInboundHandler}
import org.slf4j.LoggerFactory
import akka.actor.ActorRef
import io.netty.channel.socket.SocketChannel
import messages.Disconnected

/**
 * User: Tomas
 * Date: 28.12.13
 * Time: 14:10
 */
class ServerHandler(producerActorRef: ActorRef, channel: SocketChannel) extends SimpleChannelInboundHandler[Character] {
  import ServerHandler.logger

  def messageReceived(ctx: ChannelHandlerContext, msg: Character) {
  }

  override def channelActive(ctx: ChannelHandlerContext) {
  }

  override def channelInactive(ctx: ChannelHandlerContext) {
    producerActorRef ! Disconnected
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
    logger.error("Unexpected exception from downstream", cause)
    ctx.close()
  }
}

object ServerHandler {
  val logger = LoggerFactory.getLogger(classOf[ServerHandler])

  def factory() = (producerActorRef: ActorRef, ch: SocketChannel) => new ServerHandler(producerActorRef, ch)
}