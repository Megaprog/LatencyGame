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
    selector.connect(new InetSocketAddress(host, port), new ConnectCallback {

      def fail(e: Exception): Unit = {
        log.error(e, "Error during connect {}", self)
        self ! PoisonPill
      }

      def beforeSocketConnect(socket: Socket): Unit = {}

      def connecting(socketChannel: SocketChannel): Unit = {}

      def connected(socketChannel: SocketChannel, channelInterest: ChannelInterest): ChannelIOCallback = {
        self ! Connected(socketChannel, channelInterest)

        new ChannelIOCallback {
          def channelReadable(socketChannel: SocketChannel): Unit = self ! Read
          def ChannelWritable(socketChannel: SocketChannel): Unit = self ! Write
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
    val readBuffer = ByteBuffer.allocateDirect(1024)
    val reader = new BufferedReader(new InputStreamReader(new InputStream(){
      def read(): Int = if (readBuffer.hasRemaining) readBuffer.get & 0xff else -1
    }, DefaultCharset))
    val writeBuffer = ByteBuffer.allocateDirect(1024)
    val writer = new OutputStreamWriter(new OutputStream {
      def write(b: Int): Unit = writeBuffer.put(b.asInstanceOf[Byte])
    }, DefaultCharset)

    {
      case fromIntellect: String =>
        try {
          writer.write(fromIntellect, 0, fromIntellect.length)

        }
        catch {
          case e: IOException => log.error(e, "Error during writing to the buffer")
        }

      case Read =>
        readBuffer.clear

        var result: Int = -2
        try {
          result = socketChannel.read(readBuffer)
        }
        catch {
          case e: IOException => log.error(e, "Error during reading from the channel")
        }

        if (result < 0) {
          disconnect(socketChannel)
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
                log.error(e, "Error during convert bytes to string")
                false
            }
          } match {
            case true => channelInterest.enableReadInterest()
            case false => disconnect(socketChannel)
          }
        }

      case Write =>
        writeBuffer.flip

        var result: Int = -2
        try {
          result = socketChannel.write(writeBuffer)
        }
        catch {
          case e: IOException => log.error(e, "Error during reading from the channel")
        }

        if (result < 0) {
          disconnect(socketChannel)
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

  def disconnect(socketChannel: SocketChannel) {
    socketChannel.close()
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
  val DefaultCharset = Charset.forName("UTF-8")

  def factory(actorSystem: ActorRefFactory, host: String, port: Int, intellects: java.util.List[() => BotIntellect], selector: SelectorHandler) = (producerRef: ActorRef) =>
    actorSystem.actorOf(Props(classOf[BotActor], host, port, producerRef, intellects.get(Random.nextInt(intellects.size())).apply(), selector))

  case class Connected(socketChannel: SocketChannel, channelInterest: ChannelInterest)
  case object Read
  case object Write
}

