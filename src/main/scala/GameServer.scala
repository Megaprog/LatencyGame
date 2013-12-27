/*
 * Copyright (C) 2013 Tomas Shestakov. <https://github.com/Megaprog/LatencyGame>
 */

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.nio.NioEventLoopGroup
import java.net.InetAddress

/**
 * User: Tomas
 * Date: 27.12.13
 * Time: 17:27
 */
class GameServer(val host: InetAddress, val port: Int) extends Runnable {

  def run() {

    try {
      val bootstrap = new ServerBootstrap()
      bootstrap.group(new NioEventLoopGroup())
    }
  }
}
