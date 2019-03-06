package ru.primetalk.config.example.echo

import ru.primetalk.config.example.meta.Meta

import scala.concurrent.duration.FiniteDuration

object api extends Meta {

  type EchoProtocol[A] = JsonHttpRest[A, A]

  trait EchoConfig[A] extends ServiceRoleConfig {
    def echoPort: Port[EchoProtocol[A]]
    def echoService: HttpUrlEndPoint[NodeId, EchoProtocol[A]] = providedService(echoPort, "echo")
  }

  trait EchoClientConfig[A] {
    def pollInterval: FiniteDuration
    def echoServiceDependency: HttpUrlEndPoint[_, EchoProtocol[A]]
  }

}
