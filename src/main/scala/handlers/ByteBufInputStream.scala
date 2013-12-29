/*
 * Copyright (C) 2013 Tomas Shestakov. <https://github.com/Megaprog/LatencyGame>
 */

package handlers

import java.io.InputStream
import java.nio.ByteBuffer
import io.netty.buffer.ByteBuf

/**
 * User: Tomas
 * Date: 29.12.13
 * Time: 22:41
 */
class ByteBufInputStream(buf: ByteBuf) extends InputStream {

  def read: Int = {
    if (buf.isReadable) buf.readByte() & 0xff else -1
  }
}

