/*
 * Copyright (C) 2013 Tomas Shestakov. <https://github.com/Megaprog/LatencyGame>
 */

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.{EventLoopGroup, ChannelHandler, ChannelOption}
import io.netty.channel.socket.nio.NioServerSocketChannel
import java.net.InetAddress
import org.slf4j.LoggerFactory

/**
 * User: Tomas
 * Date: 27.12.13
 * Time: 17:27
 */
class NettyServer(val host: InetAddress, val port: Int,
                  parentGroup: EventLoopGroup, childGroup: EventLoopGroup, val childHandler: ChannelHandler) extends Server {

  def start() = {
    var exitCode = 0

    try {
      val bootstrap = new ServerBootstrap()
          .group(parentGroup, childGroup)
          .channel(classOf[NioServerSocketChannel])
          .childHandler(childHandler)
              .childOption(ChannelOption.SO_KEEPALIVE, true.asInstanceOf[java.lang.Boolean])

      bootstrap.bind(port).sync().channel().closeFuture().sync()
    }
    catch {
      case ex: Exception =>
        LoggerFactory.getLogger(classOf[NettyServer]).error(ex.getMessage, ex)
        exitCode = 1
    }
    finally {
      parentGroup.shutdownGracefully().get()
      childGroup.shutdownGracefully().get()
    }

    exitCode
  }
}
