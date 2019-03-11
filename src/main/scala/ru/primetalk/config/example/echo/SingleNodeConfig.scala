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

  // configuration of server

  type NodeId = Singleton.type

  def nodeId = Singleton

  /** Type safe service port specification */
  override def portNumber: PortNumber = 8088

  // configuration of client

  /** We'll use the service provided by the same host. */
  def echoServiceDependency = echoService

  override def testMessage: UrlPathElement = "hello"

  def pollInterval: FiniteDuration = 1.second

  // lifecycle controller configuration
  // additional 0.5 seconds so that there are 10 requests, not 9.
  def lifetime: FiniteDuration = 10500.milliseconds
}
