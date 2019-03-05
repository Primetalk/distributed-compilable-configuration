package ru.primetalk.config.example.echo

import api._
import eu.timepit.refined.auto.autoRefineV

import scala.concurrent.duration.{DurationInt, FiniteDuration}

/** It's a configuration for a single node that will implement both echo client and server.
  */
object SingleNodeConfig extends NodeConfig
  with EchoConfig[String]
  with EchoClientConfig[String]
{
  def nodeId = Singleton

  def echoPort = Port[EchoProtocol[String]](8081)

  def echoServiceDependency = echoService

  def pollInterval: FiniteDuration = 1.second
}
