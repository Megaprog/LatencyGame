/*
 * Copyright (C) 2014 Tomas Shestakov. <https://github.com/Megaprog/LatencyGame>
 */

import akka.actor._
import java.io._
import java.net.{Socket, InetSocketAddress}
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel
import java.nio.charset.Charset
import messages.{BotDisconnected, BotConnected}
import network.SelectorHandler
import network.SelectorHandler.{ChannelIOCallback, ChannelInterest, ConnectCallback}
import pipeline.{TelnetNegotiationCutter, LineFraming}
import scala.util.Random

/**
 * User: Tomas
 * Date: 06.01.14
 * Time: 13:06
 */
class BotActor(host: String, port: Int, producerRef: ActorRef, intellect: BotIntellect, selector: SelectorHandler) extends Actor with ActorLogging {
  import BotActor._

  override def preStart() {
    log.debug("intellect is {}", intellect)

    selector.connect(new InetSocketAddress(host, port), new ConnectCallback {

      def fail(e: Exception): Unit = {
        if (!e.getMessage.contains("maximum connections reached")) {
          log.error(e, "Connection error {}", self)
        }
        self ! PoisonPill
      }

      def beforeSocketConnect(socket: Socket): Unit = {}

      def connecting(socketChannel: SocketChannel): Unit = {}

      def connected(socketChannel: SocketChannel, channelInterest: ChannelInterest): ChannelIOCallback = {
        self ! Connected(socketChannel, channelInterest)

        new ChannelIOCallback {
          def channelReadable(): Unit = self ! Read
          def ChannelWritable(): Unit = self ! Write
        }
      }
    })
  }

  def receive: Actor.Receive = {

    case Connected(socketChannel, channelInterest) =>
      producerRef ! BotConnected
      intellect.attach((msg: String) => self ! msg)

      context become connected(socketChannel, channelInterest)
  }

  def connected(socketChannel: SocketChannel, channelInterest: ChannelInterest): Actor.Receive = {
    val readBuffer = ByteBuffer.allocate(256)
    val reader = new BufferedReader(new InputStreamReader(new InputStream(){
      def read(): Int = if (readBuffer.hasRemaining) readBuffer.get & 0xff else -1
    }, charset))
    val writeBuffer = ByteBuffer.allocate(128)

    {
      case fromIntellect: String =>
        log.debug("from intellect '{}' [{}]", fromIntellect, fromIntellect.map(_.toInt).mkString)

        try {
          writeBuffer.put(charset.encode(fromIntellect))
          channelInterest.enableWriteInterest()
        }
        catch {
          case e: IOException => log.error(e, "Error during writing string to the buffer")
          disconnect(socketChannel, reader)
        }

      case Read =>
        readBuffer.clear

        var result: Int = -2
        try {
          result = socketChannel.read(readBuffer)
        }
        catch {
          case e: IOException =>
            if (!e.getMessage.contains("Программа на вашем хост-компьютере разорвала установленное подключение")
                  && !e.getMessage.contains("Удаленный хост принудительно разорвал существующее подключение")) {
              log.error(e, "Error during reading from the channel")
            }
        }

        if (result < 0) {
          disconnect(socketChannel, reader)
        }
        else {
          if (result > 0) {
            readBuffer.flip()
            try {
              extractStrings(readBuffer, reader)
              true
            }
            catch {
              case e: IOException =>
                log.error(e, "Error during extracting strings from buffer")
                false
            }
          } match {
            case true => channelInterest.enableReadInterest()
            case false => disconnect(socketChannel, reader)
          }
        }

      case Write =>
        writeBuffer.flip

        var result: Int = -2
        try {
          result = socketChannel.write(writeBuffer)
        }
        catch {
          case e: IOException => log.error(e, "Error during writing to the channel")
        }

        if (result < 0) {
          disconnect(socketChannel, reader)
        }
        else {
          if (writeBuffer.hasRemaining) {
            writeBuffer.compact
            channelInterest.enableWriteInterest()
          }
          else {
            writeBuffer.clear
          }
        }
    }
  }

  def disconnect(socketChannel: SocketChannel, reader: Reader) {
    socketChannel.close()
    reader.close()

    log.debug("disc from {}", sender)

    producerRef ! BotDisconnected
    context stop self
    context become PartialFunction.empty
  }
  
  def extractStrings(buffer: ByteBuffer, reader: BufferedReader) {
    skipNegotiation(buffer
    )
    //very simple string extraction not for production
    while (true) {
      val string: String = reader.readLine
      if (string == null) {
        return
      }

      log.debug("received {}", string)

      intellect.receive(string)
    }
  }

  def skipNegotiation(buffer: ByteBuffer) {
    //skip telnet negotiation
    while (buffer.hasRemaining) {
      if (buffer.get != 0xff.toByte) {
        buffer.position(buffer.position - 1)
        return
      }

      if (buffer.hasRemaining) {
        buffer.get
      }
      if (buffer.hasRemaining) {
        buffer.get
      }
    }
  }
}

object BotActor {
  val charset = Charset.forName("UTF-8")

  def factory(actorSystem: ActorRefFactory, host: String, port: Int, intellects: java.util.List[() => BotIntellect], selector: SelectorHandler) = (producerRef: ActorRef) =>
    actorSystem.actorOf(Props(classOf[BotActor], host, port, producerRef, intellects.get(Random.nextInt(intellects.size())).apply(), selector))

  case class Connected(socketChannel: SocketChannel, channelInterest: ChannelInterest)
  case object Read
  case object Write
}

