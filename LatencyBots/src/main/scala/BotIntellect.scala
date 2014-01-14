/*
 * Copyright (C) 2014 Tomas Shestakov. <https://github.com/Megaprog/LatencyGame>
 */

/**
 * User: Tomas
 * Date: 07.01.14
 * Time: 10:41
 */
trait Intellect[Input, Output] {

  type Writer = (Output) => Unit

  def receive(data: Input)

  def attach(writer: Writer)
}

trait BotIntellect extends Intellect[String, String] {
  
  val WaitingString = "3"
  
  val WinningString = " " //space
}

class SilentIntellect extends BotIntellect {
  //do nothing

  def receive(data: String) {} //do nothing

  def attach(writer: Writer) {} //do nothing
}
object SilentIntellect {

  val factory = () => new SilentIntellect
}

class QuickIntellect extends BotIntellect {
  //writes after second receive

  var writerOption = Option.empty[Writer]
  var counter = 2

  def receive(data: String) {
    counter -= 1
    if (counter == 0) {
      writerOption foreach (_(WinningString))
    }
  }

  def attach(writer: Writer) { writerOption = Some(writer) }
}
object QuickIntellect {

  val factory = () => new QuickIntellect
}

class CleverIntellect extends BotIntellect {
  //writes after receive WaitingString

  var writerOption = Option.empty[Writer]
  
  def receive(data: String) {
    data match {
      case WaitingString => writerOption foreach (_(WinningString))
      case _ =>
    }
  }

  def attach(writer: Writer) { writerOption = Some(writer) }
}
object CleverIntellect {

  val factory = () => new CleverIntellect
}