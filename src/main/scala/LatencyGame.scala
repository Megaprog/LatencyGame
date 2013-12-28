/*
 * Copyright (C) 2013 Tomas Shestakov. <https://github.com/Megaprog/LatencyGame>
 */

import org.springframework.context.ApplicationContext
import org.springframework.context.support.{ClassPathXmlApplicationContext, FileSystemXmlApplicationContext}

/**
 * User: Tomas
 * Date: 27.12.13
 * Time: 17:25
 */
object LatencyGame extends App {
  val DefaultContextName = "app-context.xml"

  val context: ApplicationContext = 
    if (args.length > 0) new FileSystemXmlApplicationContext(args(0)) 
    else new ClassPathXmlApplicationContext(DefaultContextName)

  val server = context.getBean("server", classOf[Runnable])

  server.run()
}
