/*
 * Copyright (C) 2013 Tomas Shestakov. <https://github.com/Megaprog/LatencyGame>
 */

import akka.actor.ActorRef
import io.netty.bootstrap.Bootstrap
import io.netty.channel.{EventLoopGroup, ChannelHandler, ChannelOption}
import io.netty.channel.socket.nio.NioSocketChannel
import org.slf4j.LoggerFactory

/**
 * User: Tomas
 * Date: 27.12.13
 * Time: 17:27
 */
class NettyClient(host: String, port: Int,
                  eventGroup: EventLoopGroup, initializer: ChannelHandler) extends Runnable {

  def run() {

    try {
      val bootstrap = new Bootstrap()
          .group(eventGroup)
          .channel(classOf[NioSocketChannel])
          .option[java.lang.Boolean](ChannelOption.SO_KEEPALIVE, true)
          .handler(initializer)

      bootstrap.connect(host, port).sync().channel().closeFuture().sync()
    }
    catch {
      case ex: Exception =>
        LoggerFactory.getLogger(classOf[NettyClient]).error(ex.getMessage, ex)
    }
    finally {
      eventGroup.shutdownGracefully().get()
    }

  }
}

object NettyClient {

  def factory(host: String, port: Int, eventGroup: EventLoopGroup, initializerFactory: (ActorRef) => ChannelHandler) = (producerRef: ActorRef) => new NettyClient(host, port, eventGroup, initializerFactory(producerRef))
}
