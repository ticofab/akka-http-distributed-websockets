package io.ticofab.distributedws.common

import akka.actor.ActorRef
import akka.stream.SourceRef

sealed trait Message

object Messages {

  case object RegisterNode extends Message
  case object ProvideConnectionHandler extends Message
  case class ConnectionHandler(handlingActor: ActorRef, sourceRef: SourceRef[String]) extends Message

}
