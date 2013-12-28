/*
 * Copyright (C) 2013 Tomas Shestakov. <https://github.com/Megaprog/LatencyGame>
 */

import io.netty.channel.socket.SocketChannel
import io.netty.channel.{ChannelPromise, ChannelHandlerContext, ChannelInitializer}

/**
 * User: Tomas
 * Date: 27.12.13
 * Time: 22:24
 */
class GamePipelineInitializer extends ChannelInitializer[SocketChannel] {

  def initChannel(ch: SocketChannel) {

  }

  override def close(ctx: ChannelHandlerContext, promise: ChannelPromise) {
    println("GamePipelineInitializer closing")
    super.close(ctx, promise)
  }
}
