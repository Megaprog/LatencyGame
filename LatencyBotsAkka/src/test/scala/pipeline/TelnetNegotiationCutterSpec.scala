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
class TelnetNegotiationCutterSpec extends WordSpec with Matchers {

  "TelnetNegotiationCutter" should {

    "provide an 'apply' method which returns PipePair" which {

      val cutterPipePair = new TelnetNegotiationCutter().apply(new PipelineContext {})

      "has commandPipeline function" which {
        "returns single command with ByteString argument" in {
          cutterPipePair.commandPipeline(ByteString(1,2,3)).head should be(Left(ByteString(1,2,3)).left.get)
        }
      }

      "has eventPipeline function" which {
        "returns single event with ByteString without Telnet negotiation bytes sequence" in {
          cutterPipePair.eventPipeline(ByteString(1,2,3)) should be(Iterable(Left(ByteString(1,2,3))))

          cutterPipePair.eventPipeline(ByteString(0xff,2,3)) should be(Iterable())

          cutterPipePair.eventPipeline(ByteString(0xff)) should be(Iterable())
          cutterPipePair.eventPipeline(ByteString(2)) should be(Iterable())
          cutterPipePair.eventPipeline(ByteString(3)) should be(Iterable())
          cutterPipePair.eventPipeline(ByteString(4)) should be(Iterable(Left(ByteString(4))))

          cutterPipePair.eventPipeline(ByteString(0xff, 2)) should be(Iterable())
          cutterPipePair.eventPipeline(ByteString(3)) should be(Iterable())
          cutterPipePair.eventPipeline(ByteString(4)) should be(Iterable(Left(ByteString(4))))
        }
      }
    }
  }
}
