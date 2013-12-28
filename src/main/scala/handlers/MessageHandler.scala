package handlers

import io.netty.channel.{ChannelHandlerContext, SimpleChannelInboundHandler}
import org.slf4j.LoggerFactory

/*
 * Copyright (C) 2013 Tomas Shestakov. <https://github.com/Megaprog/LatencyGame>
 */

/**
 * User: Tomas
 * Date: 28.12.13
 * Time: 14:10
 */
class MessageHandler extends SimpleChannelInboundHandler[String] {

  def messageReceived(ctx: ChannelHandlerContext, msg: String) {
    println(msg)
  }

  override def channelActive(ctx: ChannelHandlerContext): Unit = super.channelActive(ctx)

  override def channelInactive(ctx: ChannelHandlerContext): Unit = super.channelInactive(ctx)

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
    LoggerFactory.getLogger(classOf[MessageHandler].getName).error("Unexpected exception from downstream.", cause)
    ctx.close()
  }
}
