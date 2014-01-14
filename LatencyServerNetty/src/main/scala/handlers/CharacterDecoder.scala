/*
 * Copyright (C) 2013 Tomas Shestakov. <https://github.com/Megaprog/LatencyGame>
 */

package handlers

import io.netty.handler.codec.ByteToMessageDecoder
import io.netty.channel.ChannelHandlerContext
import io.netty.buffer.ByteBuf
import java.util
import java.io.InputStreamReader
import java.nio.charset.Charset

/**
 * User: Tomas
 * Date: 29.12.13
 * Time: 21:31
 */
class CharacterDecoder(charset: Charset) extends ByteToMessageDecoder {

  def decode(ctx: ChannelHandlerContext, in: ByteBuf, out: util.List[AnyRef]) {
    NegotiatedLineBasedFrameDecoder.skipNegotiation(in)

    if (in.isReadable) {
      val reader = new InputStreamReader(new ByteBufInputStream(in), charset)

      while (true) {
        val char = reader.read()
        if (char == -1) {
          return
        }

        out.add(Character.valueOf(char.toChar))
      }
    }
  }
}
