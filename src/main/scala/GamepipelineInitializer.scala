/*
 * Copyright (C) 2013 Tomas Shestakov. <https://github.com/Megaprog/LatencyGame>
 */

import handlers.{NegotiatedLineBasedFrameDecoder, MessageHandler}
import io.netty.channel.socket.SocketChannel
import io.netty.channel.{ChannelPipeline, ChannelInitializer}
import io.netty.handler.codec.string.{StringEncoder, StringDecoder}
import io.netty.handler.codec.LineBasedFrameDecoder

/**
 * User: Tomas
 * Date: 27.12.13
 * Time: 22:24
 */
class GamePipelineInitializer extends ChannelInitializer[SocketChannel] {

  def initChannel(ch: SocketChannel) {
    val pipeline: ChannelPipeline = ch.pipeline

    //Remove telnet negotiation bytes if it present and split text by lines
    pipeline.addLast("framer", new NegotiatedLineBasedFrameDecoder(1024))

    pipeline.addLast("decoder", GamePipelineInitializer.Decoder)

    //Business logic
    pipeline.addLast("handler", new MessageHandler)

    pipeline.addLast("encoder", GamePipelineInitializer.Encoder)
  }
}

object GamePipelineInitializer {
  //The encoder and decoder are static as these are sharable
  val Decoder = new StringDecoder
  val Encoder = new StringEncoder
}
