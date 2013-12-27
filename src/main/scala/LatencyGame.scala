/*
 * Copyright (C) 2013 Tomas Shestakov. <https://github.com/Megaprog/LatencyGame>
 */

/**
 * User: Tomas
 * Date: 27.12.13
 * Time: 17:25
 */
object LatencyGame extends App {
  val DefaultPort = 8080

  val port = if (args.length > 0) args(0).toInt else DefaultPort

  new GameServer(null, port).run()
}
