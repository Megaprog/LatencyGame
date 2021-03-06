/*
 * Copyright (C) 2013 Tomas Shestakov. <https://github.com/Megaprog/LatencyGame>
 */

import akka.actor.{ActorRef, ActorSystem}
import org.springframework.context.ApplicationContext
import org.springframework.context.support.{ClassPathXmlApplicationContext, FileSystemXmlApplicationContext}

/**
 * User: Tomas
 * Date: 27.12.13
 * Time: 17:25
 */
object LatencyServerAkka extends App {
  val DefaultContextName = "app-context.xml"

  val context: ApplicationContext = 
    if (args.length > 0) new FileSystemXmlApplicationContext(args(0)) 
    else new ClassPathXmlApplicationContext(DefaultContextName)

  context.getBean(classOf[ActorSystem]).registerOnTermination({System.exit(1)})

  context.getBean("networkServer", classOf[ActorRef])
}
