package ru.primetalk.config.example.echo

import api._
import eu.timepit.refined.auto.autoRefineV

import scala.concurrent.duration.{DurationInt, FiniteDuration}

/** It's a configuration for a single node that will
  * implement both echo client and server.
  */
object SingleNodeConfig
  extends EchoConfig[String]
  with EchoClientConfig[String]
  with LifecycleManagerConfig
{
  def nodeId = Singleton

  def echoPort = Port[EchoProtocol[String]](8081)

  def echoServiceDependency = echoService

  def pollInterval: FiniteDuration = 1.second

  def lifetime: FiniteDuration = 10.seconds
}
