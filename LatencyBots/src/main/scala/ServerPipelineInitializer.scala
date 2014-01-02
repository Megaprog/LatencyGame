/*
 * Copyright (C) 2014 Tomas Shestakov. <https://github.com/Megaprog/LatencyGame>
 */

import akka.actor.ActorRef
import handlers.NegotiatedLineBasedFrameDecoder
import io.netty.channel.socket.SocketChannel
import io.netty.channel.{ChannelHandler, ChannelPipeline, ChannelInitializer}
import io.netty.handler.codec.string.{StringEncoder, StringDecoder}
import java.nio.charset.Charset

/**
 * User: Tomas
 * Date: 27.12.13
 * Time: 22:24
 */
class ServerPipelineInitializer(messageHandlerFactory: (SocketChannel) => ChannelHandler) extends ChannelInitializer[SocketChannel] {

  def initChannel(ch: SocketChannel) {
    val pipeline: ChannelPipeline = ch.pipeline

    pipeline.addLast("framer", new NegotiatedLineBasedFrameDecoder(512))

    pipeline.addLast("decoder", ServerPipelineInitializer.Decoder)
    pipeline.addLast("encoder", ServerPipelineInitializer.Encoder)

    //Business logic
    pipeline.addLast("handler", messageHandlerFactory(ch))
  }
}

object ServerPipelineInitializer {
  val StringCharset = Charset.forName("UTF-8")

  //The encoder and decoder are static as these are sharable
  val Decoder = new StringDecoder(StringCharset)
  val Encoder = new StringEncoder(StringCharset)

  def factory(serverHandlerFactory: (ActorRef, SocketChannel) => ChannelHandler) = (producerRef: ActorRef) => new ServerPipelineInitializer((ch: SocketChannel) => serverHandlerFactory(producerRef, ch))
}
