package ru.primetalk.config.example.echo

import ru.primetalk.config.example.meta.Meta

import scala.language.implicitConversions

object api extends Meta {

  type FiniteDuration = scala.concurrent.duration.FiniteDuration
  implicit def durationInt(i: Int): scala.concurrent.duration.DurationInt = new scala.concurrent.duration.DurationInt(i)

  type EchoProtocol[A] = SimpleHttpGetRest[A, A]

  trait EchoConfig[A] extends ServiceConfig {
    def echoPort: Port[EchoProtocol[A]]
    def echoService: HttpSimpleGetEndPoint[NodeId, EchoProtocol[A]] = providedSimpleService(echoPort, "echo")
  }

  trait EchoClientConfig[A] {
    def testMessage: String = "test"
    def pollInterval: FiniteDuration
    def echoServiceDependency: HttpSimpleGetEndPoint[_, EchoProtocol[A]]
  }

}
