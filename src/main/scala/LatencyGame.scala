/*
 * Copyright (C) 2013 Tomas Shestakov. <https://github.com/Megaprog/LatencyGame>
 */

import akka.actor.ActorSystem
import executors.ExecutorToRunner
import io.netty.channel.nio.NioEventLoopGroup

/**
 * User: Tomas
 * Date: 27.12.13
 * Time: 17:25
 */
object LatencyGame extends App {
  val DefaultPort = 8080

  val system = ActorSystem()

  val port = if (args.length > 0) args(0).toInt else DefaultPort

  new NettyServer(null, port,
    new NioEventLoopGroup(1, new ExecutorToRunner(system, 1)),
    new NioEventLoopGroup(4, new ExecutorToRunner(system, 4)),
    new GamePipelineInitializer
  ).run()
}
