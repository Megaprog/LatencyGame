/*
 * Copyright (C) 2014 Tomas Shestakov. <https://github.com/Megaprog/LatencyGame>
 */

package pipeline

import akka.io.{SymmetricPipePair, PipelineContext, SymmetricPipelineStage}
import akka.util.ByteString

/**
 * User: Tomas
 * Date: 06.01.14
 * Time: 13:09
 *
 * Telnet negotiation is a sequence of triplets of bytes. Each triplet begins from 0xff byte value.
 * Negotiation may appears only at begin of message from telnet server or client.
 */
class TelnetNegotiationCutter extends SymmetricPipelineStage[PipelineContext, ByteString, ByteString] {

  override def apply(ctx: PipelineContext) = new SymmetricPipePair[ByteString, ByteString] {

    def commandPipeline: (ByteString) => Iterable[Result] = {
      bs: ByteString => ctx.singleCommand(bs)
    }

    def eventPipeline: (ByteString) => Iterable[Result] = {
      bs: ByteString => cutNegotiation(bs).map(Left(_))
    }

    protected var skipBytes = 0

    protected def cutNegotiation(bs: ByteString): Option[ByteString] = {
      if (bs.isEmpty) {
        None
      }
      else if (skipBytes > 0) {
        val skipped = math.min(skipBytes, bs.size)
        skipBytes -= skipped
        cutNegotiation(bs.drop(skipped))
      }
      else if (bs.head == 0xff.toByte) {
        skipBytes = 2
        cutNegotiation(bs.tail)
      }
      else {
        Some(bs)
      }
    }
  }
}
