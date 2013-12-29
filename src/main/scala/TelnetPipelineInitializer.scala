/*
 * Copyright (C) 2013 Tomas Shestakov. <https://github.com/Megaprog/LatencyGame>
 */

import handlers.{NegotiatedLineBasedFrameDecoder, ClientHandler}
import io.netty.channel.socket.SocketChannel
import io.netty.channel.{ChannelHandler, ChannelPipeline, ChannelInitializer}
import io.netty.handler.codec.string.{StringEncoder, StringDecoder}
import io.netty.handler.codec.LineBasedFrameDecoder
import java.nio.charset.Charset

/**
 * User: Tomas
 * Date: 27.12.13
 * Time: 22:24
 */
class TelnetPipelineInitializer(messageHandlerFactory: (SocketChannel) => ChannelHandler) extends ChannelInitializer[SocketChannel] {

  def initChannel(ch: SocketChannel) {
    val pipeline: ChannelPipeline = ch.pipeline

    //Remove telnet negotiation bytes if it present and split text by lines
    pipeline.addLast("framer", new NegotiatedLineBasedFrameDecoder(1024))

    pipeline.addLast("decoder", TelnetPipelineInitializer.Decoder)
    pipeline.addLast("encoder", TelnetPipelineInitializer.Encoder)

    //Business logic
    pipeline.addLast("handler", messageHandlerFactory(ch))
  }
}

object TelnetPipelineInitializer {
  val StringCharset = Charset.forName("UTF-8")

  //The encoder and decoder are static as these are sharable
  val Decoder = new StringDecoder(StringCharset)
  val Encoder = new StringEncoder(StringCharset)
}
