/*
 * Copyright (C) 2013 Tomas Shestakov. <https://github.com/Megaprog/LatencyGame>
 */

import akka.actor.ActorRef
import io.netty.bootstrap.Bootstrap
import io.netty.channel._
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
          .option[java.lang.Integer](ChannelOption.CONNECT_TIMEOUT_MILLIS, 500)
          .handler(initializer)

      bootstrap.connect(host, port).addListener(new ChannelFutureListener {
        def operationComplete(future: ChannelFuture) {
        }
      }).sync().channel().closeFuture().sync()
    }
    catch {
      case ex: Exception => NettyClient.logger.error(ex.getMessage, ex)
    }
    finally {
      eventGroup.shutdownGracefully()
    }
  }
}

object NettyClient {
  val logger = LoggerFactory.getLogger(classOf[NettyClient])

  def factory(host: String, port: Int, eventGroup: EventLoopGroup, initializerFactory: (ActorRef) => ChannelHandler) = (producerRef: ActorRef) => new NettyClient(host, port, eventGroup, initializerFactory(producerRef))
}
