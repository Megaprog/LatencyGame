/*
 * Copyright (C) 2013 Tomas Shestakov. <https://github.com/Megaprog/LatencyGame>
 */

package handlers

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.{LineBasedFrameDecoder, ByteToMessageDecoder}

/**
 * User: Tomas
 * Date: 28.12.13
 * Time: 20:24
 */
class NegotiatedLineBasedFrameDecoder(maxLength: Int, stripDelimiter: Boolean, failFast: Boolean)
    extends LineBasedFrameDecoder(maxLength, stripDelimiter, failFast) {

  def this(maxLength: Int) = this(maxLength, true, false)

  override def decode(ctx: ChannelHandlerContext, buffer: ByteBuf): AnyRef = {
    skipNegotiation(buffer)
    super.decode(ctx, buffer)
  }

  def skipNegotiation(in: ByteBuf) {
    while (in.isReadable) {
      in.markReaderIndex()

      if (in.readByte() == 0xff.toByte) {
        skipByte(in)
        skipByte(in)
      }
      else {
        in.resetReaderIndex()
        return
      }
    }
  }

  def skipByte(in: ByteBuf) {
    if (in.isReadable) {
      in.readByte()
    }
  }
}
