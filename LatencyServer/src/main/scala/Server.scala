/*
 * Copyright (C) 2013 Tomas Shestakov. <https://github.com/Megaprog/LatencyGame>
 */

/**
 * User: Tomas
 * Date: 30.12.13
 * Time: 15:13
 */
trait Server {

  /**
   * @return system exit code. When server returns from this method application will be shutdown.
   */
  def start(): Int
}
