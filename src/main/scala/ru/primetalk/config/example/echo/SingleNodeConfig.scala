package ru.primetalk.config.example.echo

import api._

/** It's a configuration for a single node that will
  * implement both echo client and server.
  */
object SingleNodeConfig
  extends EchoConfig[String]
  with EchoClientConfig[String]
  with FiniteDurationLifecycleConfig
{
  case object Singleton

  type NodeId = Singleton.type

  def nodeId = Singleton

  def echoPort = Port[EchoProtocol[String]](8081)

  def echoServiceDependency = echoService

  def pollInterval: FiniteDuration = 1.second

  def lifetime: FiniteDuration = 10500.milliseconds // additional 0.5 seconds so that there are 10 request, not 9.
}
