/*
 * Copyright (C) 2013 Tomas Shestakov. <https://github.com/Megaprog/LatencyGame>
 */

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.{EventLoopGroup, ChannelHandler, ChannelOption}
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import java.net.InetAddress

/**
 * User: Tomas
 * Date: 27.12.13
 * Time: 17:27
 */
class NettyServer(val host: InetAddress, val port: Int,
                  parentGroup: EventLoopGroup, childGroup: EventLoopGroup, val childHandler: ChannelHandler) extends Runnable {

  def run() {

    try {
      val bootstrap = new ServerBootstrap()
          .group(parentGroup, childGroup)
          .channel(classOf[NioServerSocketChannel])
          .childHandler(childHandler)
              .childOption(ChannelOption.SO_KEEPALIVE, true.asInstanceOf[java.lang.Boolean])

      bootstrap.bind(port).sync().channel().closeFuture().sync()
    }
    finally {
      parentGroup.shutdownGracefully()
      childGroup.shutdownGracefully()
    }

  }
}
