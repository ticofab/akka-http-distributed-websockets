package io.ticofab.distributedws

import akka.actor.{Actor, ActorSystem, Props, RootActorPath}
import akka.cluster.Cluster
import akka.pattern.pipe
import akka.stream.scaladsl.{Keep, Sink, Source, StreamRefs}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import com.typesafe.config.ConfigFactory
import io.ticofab.distributedws.common.Messages.{ConnectionHandler, ProvideConnectionHandler, RegisterNode}
import wvlet.log.LogFormatter.SourceCodeLogFormatter
import wvlet.log.{LogLevel, LogSupport, Logger}

import scala.concurrent.ExecutionContext.Implicits.global

object HandlerApp extends App with LogSupport {
  Logger.setDefaultFormatter(SourceCodeLogFormatter)
  Logger.setDefaultLogLevel(LogLevel.DEBUG)
  info("listener starting")

  implicit val as = ActorSystem("distributed-websockets")

  val port = ConfigFactory.load().getInt("akka.remote.netty.tcp.port")
  as.actorOf(Props[Handler], s"node$port")

}

class Handler extends Actor with LogSupport {

  implicit val as = context.system
  implicit val am = ActorMaterializer()

  info(s"handler actor ${self.path.name} started.")

  // make sure that we register only we we are effectively up
  val cluster = Cluster(as)
  cluster registerOnMemberUp {
    cluster.state.leader.foreach(leaderAddress =>
      as.actorSelection(RootActorPath(leaderAddress) / "user" / "supervisor") ! RegisterNode)
  }

  // every message this actor sends to the down actorRef will end up in the publisher sink...
  val (down, publisher) = Source
    .actorRef[String](1000, OverflowStrategy.fail)
    .toMat(Sink.asPublisher(fanout = false))(Keep.both)
    .run()

  // ... and every message in the publisher sink will be published by this Source. The final effect is
  // that messages sent to the down actorRef will be published to this source.
  val streamRef = Source.fromPublisher(publisher).runWith(StreamRefs.sourceRef())

  override def receive: Receive = {

    case ProvideConnectionHandler =>
      // send the streamRef reference back
      streamRef.map(ConnectionHandler(self, _)).pipeTo(sender)

    case s: String =>
      // reply with a salute (assuming someone sent us his/her name
      debug(s"Received message: $s")
      down ! "Hello " + s + "!"
  }

}
