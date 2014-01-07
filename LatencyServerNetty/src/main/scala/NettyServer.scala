/*
 * Copyright (C) 2013 Tomas Shestakov. <https://github.com/Megaprog/LatencyGame>
 */

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.{EventLoopGroup, ChannelHandler, ChannelOption}
import io.netty.channel.socket.nio.NioServerSocketChannel
import java.net.{InetSocketAddress, InetAddress}
import org.slf4j.LoggerFactory

/**
 * User: Tomas
 * Date: 27.12.13
 * Time: 17:27
 */
class NettyServer(port: Int,
                  parentGroup: EventLoopGroup, childGroup: EventLoopGroup,
                  initializer: ChannelHandler) extends Server {

  def start() = {

    try {
      val bootstrap = new ServerBootstrap()
          .group(parentGroup, childGroup)
          .channel(classOf[NioServerSocketChannel])
          .childHandler(initializer)
              .childOption[java.lang.Boolean](ChannelOption.SO_KEEPALIVE, true)

      bootstrap.bind(port).sync().channel().closeFuture().sync()
      0
    }
    catch {
      case ex: Exception =>
        LoggerFactory.getLogger(classOf[NettyServer]).error(ex.getMessage, ex)
        1
    }
    finally {
      parentGroup.shutdownGracefully().get()
      childGroup.shutdownGracefully().get()
    }
  }
}
