/*
 * Copyright (C) 2013 Tomas Shestakov. <https://github.com/Megaprog/LatencyGame>
 */

import handlers.{CharacterDecoder, NegotiatedLineBasedFrameDecoder, ClientHandler}
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
class ClientPipelineInitializer(messageHandlerFactory: (SocketChannel) => ChannelHandler) extends ChannelInitializer[SocketChannel] {

  def initChannel(ch: SocketChannel) {
    val pipeline: ChannelPipeline = ch.pipeline

    pipeline.addLast("decoder", new CharacterDecoder(ClientPipelineInitializer.StringCharset))
    pipeline.addLast("encoder", ClientPipelineInitializer.Encoder)

    //Business logic
    pipeline.addLast("handler", messageHandlerFactory(ch))
  }
}

object ClientPipelineInitializer {
  val StringCharset = Charset.forName("UTF-8")

  //The encoder and decoder are static as these are sharable
  val Decoder = new StringDecoder(StringCharset)
  val Encoder = new StringEncoder(StringCharset)
}
