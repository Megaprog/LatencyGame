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
class LineFraming(maxSize: Int) extends SymmetricPipelineStage[PipelineContext, ByteString, ByteString] {
  require(maxSize > 0, "maxSize must be positive")

  val delimiter1 = '\n'.toByte
  val delimiter2 = '\r'.toByte

  override def apply(ctx: PipelineContext) = new SymmetricPipePair[ByteString, ByteString] {

    def commandPipeline: (ByteString) => Iterable[Result] = {
      bs: ByteString => ctx.singleCommand(bs)
    }

    def eventPipeline: (ByteString) => Iterable[Result] = {
      bs: ByteString => extractLines(bs, Nil).reverseMap(Left(_))
    }

    var buffer = ByteString.empty
    var waitingSecondDelimiter = false

    protected def extractLines(nextChunk: ByteString, acc: List[ByteString]): List[ByteString] = {
      val matchPosition1 = nextChunk.indexOf(delimiter1)
      val matchPosition2 = nextChunk.indexOf(delimiter2)
      val matchPosition = if (matchPosition1 == -1) matchPosition2 else if (matchPosition2 == -1) matchPosition1 else math.min(matchPosition1, matchPosition2)

      if (matchPosition == -1) {
        waitingSecondDelimiter = false
        if (buffer.size + nextChunk.size > maxSize) throw new IllegalArgumentException(
          s"Received too large frame of size ${buffer.size + nextChunk.size} (max = $maxSize)")

        buffer ++= nextChunk
        acc
      }
      else if (matchPosition == 0 && waitingSecondDelimiter) {
          waitingSecondDelimiter = false
          extractLines(nextChunk.tail, acc)
      }
      else {
        val decoded = (buffer ++ nextChunk.take(matchPosition)) :: acc
        buffer = ByteString.empty

        var nextPosition = matchPosition + 1
        if (nextPosition >= nextChunk.size) {
          waitingSecondDelimiter = true
          decoded
        }
        else {
          waitingSecondDelimiter = false
          val nextByte = nextChunk(nextPosition)
          if (nextByte == delimiter1 || nextByte == delimiter2) {
            nextPosition += 1
          }
          extractLines(nextChunk.drop(nextPosition), decoded)
        }
      }
    }
  }
}
