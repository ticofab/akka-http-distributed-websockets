package io.ticofab.distributedws

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.http.scaladsl.model.{HttpEntity, HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives.{complete, handleWebSocketMessages, onComplete, path}
import akka.pattern.{ask, pipe}
import akka.stream.scaladsl.GraphDSL.Implicits._
import akka.stream.scaladsl.{Flow, GraphDSL, Sink}
import akka.stream.{ActorMaterializer, FlowShape}
import io.ticofab.distributedws.common.Messages.{ConnectionHandler, ProvideConnectionHandler, RegisterNode}
import wvlet.log.LogFormatter.SourceCodeLogFormatter
import wvlet.log.{LogLevel, LogSupport, Logger}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Random, Success}

object ListenerApp extends App with LogSupport {
  Logger.setDefaultFormatter(SourceCodeLogFormatter)
  Logger.setDefaultLogLevel(LogLevel.DEBUG)
  info("listener starting")

  type HandlingFlow = Flow[Message, Message, _]
  implicit val as = ActorSystem("distributed-websockets")
  implicit val am = ActorMaterializer()
  val supervisor = as.actorOf(Props[Supervisor], "supervisor")

  val routes = path("connect") {
    info(s"client connected")
    val handlingFlow = (supervisor ? ProvideConnectionHandler) (3.seconds).mapTo[HandlingFlow]
    onComplete(handlingFlow) {
      case Success(flow) => handleWebSocketMessages(flow)
      case Failure(err) => complete(HttpResponse(StatusCodes.InternalServerError, entity = HttpEntity(err.getMessage)))
    }
  }

  Http().bindAndHandle(routes, "0.0.0.0", 8080)
}

class Supervisor extends Actor with LogSupport {
  implicit val as = context.system
  implicit val am = ActorMaterializer()

  info(s"starting, $self")

  // my state: the connected nodes
  var nodes: Vector[ActorRef] = Vector()

  def receive = {
    case RegisterNode =>
      debug(s"node joined: $sender")
      nodes = nodes :+ sender

    case pch@ProvideConnectionHandler =>
      // forward it to a random node, if any
      val chosenNode = nodes(Random.nextInt(nodes.size))
      debug(s"sending request for handler to ${chosenNode.path.name}")
      (chosenNode ? pch) (3.seconds)
        .mapTo[ConnectionHandler]
        .map { case ConnectionHandler(handlingActor, sourceRef) =>

          // create and send flow back
          Flow.fromGraph(GraphDSL.create() { implicit b =>

            val textMsgFlow = b.add(Flow[Message]
              .mapAsync(1) {
                case tm: TextMessage => tm.toStrict(3.seconds).map(_.text)
                case bm: BinaryMessage =>
                  bm.dataStream.runWith(Sink.ignore)
                  Future.failed(new Exception("yuck"))
              })

            // map strings coming from this source to TextMessage
            val pubSrc = b.add(sourceRef.map(TextMessage(_)))

            // forward each message to the handling actor
            textMsgFlow ~> Sink.foreach[String](handlingActor ! _)

            FlowShape(textMsgFlow.in, pubSrc.out)
          })
        }
        .pipeTo(sender)
  }
}
