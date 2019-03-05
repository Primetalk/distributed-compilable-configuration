package ru.primetalk.config.example.echo

import ru.primetalk.config.example.meta.Meta

import scala.concurrent.duration.FiniteDuration

object api extends Meta {

  override type NodeId = NodeIdImpl

  sealed trait NodeIdImpl
  case object Singleton extends NodeIdImpl

  type EchoProtocol[A] = JsonHttpRest[A, A]

  trait EchoConfig[A] extends RoleConfig { self: NodeConfig =>
    def echoPort: Port[EchoProtocol[A]]
    def echoService: HttpUrlEndPoint[EchoProtocol[A]] = providedService(echoPort, "echo")
  }

  trait EchoClientConfig[A] extends RoleConfig { self: NodeConfig =>
    def pollInterval: FiniteDuration
    def echoServiceDependency: HttpUrlEndPoint[EchoProtocol[A]]
  }

}
