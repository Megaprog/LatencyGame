/*
 * Copyright (C) 2014 Tomas Shestakov. <https://github.com/Megaprog/LatencyGame>
 */

package pipeline

import org.scalatest.{Matchers, WordSpec}
import akka.io.PipelineContext
import akka.util.ByteString

/**
 * User: Tomas
 * Date: 06.01.14
 * Time: 15:14
 */
class LineFramingSpec extends WordSpec with Matchers {

  "LineFraming" should {

    "provide an 'apply' method which returns PipePair" which {

      val framingPipePair = new LineFraming(512).apply(new PipelineContext {})

      "has commandPipeline function" which {
        "returns single command with ByteString argument" in {
          framingPipePair.commandPipeline(ByteString(1,2,3)).head should be(Left(ByteString(1,2,3)).left.get)
        }
      }

      "has eventPipeline function" which {
        "returns ByteStrings splitted by '\\n' or '\\r' characters" in {
          framingPipePair.eventPipeline(ByteString(1,2,3)) should be(Iterable())
          framingPipePair.eventPipeline(ByteString('\n')) should be(Iterable(Left(ByteString(1,2,3))))
          framingPipePair.eventPipeline(ByteString(4,'\n')) should be(Iterable(Left(ByteString(4))))

          framingPipePair.eventPipeline(ByteString(1,2,3)) should be(Iterable())
          framingPipePair.eventPipeline(ByteString('\r')) should be(Iterable(Left(ByteString(1,2,3))))
          framingPipePair.eventPipeline(ByteString(4,'\r')) should be(Iterable(Left(ByteString(4))))

          framingPipePair.eventPipeline(ByteString(1,2,3)) should be(Iterable())
          framingPipePair.eventPipeline(ByteString('\r','\n')) should be(Iterable(Left(ByteString(1,2,3))))
          framingPipePair.eventPipeline(ByteString(4,'\r','\n')) should be(Iterable(Left(ByteString(4))))

          framingPipePair.eventPipeline(ByteString(1,2,3)) should be(Iterable())
          framingPipePair.eventPipeline(ByteString('\n','\r')) should be(Iterable(Left(ByteString(1,2,3))))
          framingPipePair.eventPipeline(ByteString(4,'\n','\r')) should be(Iterable(Left(ByteString(4))))

          framingPipePair.eventPipeline(ByteString(1,2,'\n')) should be(Iterable(Left(ByteString(1,2))))
          framingPipePair.eventPipeline(ByteString('\r')) should be(Iterable())
          framingPipePair.eventPipeline(ByteString(3,'\n')) should be(Iterable(Left(ByteString(3))))

          framingPipePair.eventPipeline(ByteString(1,2,'\r')) should be(Iterable(Left(ByteString(1,2))))
          framingPipePair.eventPipeline(ByteString('\n')) should be(Iterable())
          framingPipePair.eventPipeline(ByteString(3,'\r')) should be(Iterable(Left(ByteString(3))))
        }
      }
    }
  }
}
