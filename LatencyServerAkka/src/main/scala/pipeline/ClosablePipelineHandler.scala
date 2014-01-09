/*
 * Copyright (C) 2014 Tomas Shestakov. <https://github.com/Megaprog/LatencyGame>
 */

package pipeline

import akka.io.{TcpPipelineHandler, PipelineContext}
import akka.actor.ActorRef
import akka.io.Tcp.CloseCommand

/**
 * User: Tomas
 * Date: 08.01.14
 * Time: 10:47
 */
class ClosablePipelineHandler[Ctx <: PipelineContext, Cmd, Evt] (
    init: TcpPipelineHandler.Init[Ctx, Cmd, Evt],
    connection: ActorRef,
    handler: ActorRef)
  extends TcpPipelineHandler(init, connection, handler) {

  override def receive = super.receive orElse {
    case close: CloseCommand => connection ! close
  }
}
