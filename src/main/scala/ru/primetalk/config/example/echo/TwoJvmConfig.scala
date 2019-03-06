package ru.primetalk.config.example.echo
import api._

/** It's a configuration for a two nodes that will
  * implement echo client and server separately.
  */
object TwoJvmConfig {

  sealed trait NodeIdImpl

  case object NodeServer extends NodeIdImpl
  case object NodeClient extends NodeIdImpl

  object NodeServerConfig extends EchoConfig[String] with SigTermLifecycleConfig
  {
    type NodeId = NodeIdImpl

    def nodeId = NodeServer

    def echoPort = Port[EchoProtocol[String]](8081)
  }

  object NodeClientConfig extends EchoClientConfig[String] with FiniteDurationLifecycleConfig
  {
    def echoServiceDependency = NodeServerConfig.echoService

    def pollInterval: FiniteDuration = 1.second

    def lifetime: FiniteDuration = 10500.milliseconds // additional 0.5 seconds so that there are 10 request, not 9.
  }

}
