package ru.primetalk.config.example.echo

import cats.effect._
import api._
import scala.language.higherKinds

object SingleNodeImpl extends ZeroRoleImpl[IO]
  with EchoServiceRole
  with EchoClientRole
  with FiniteDurationLifecycleRoleImpl
{
  type Config = EchoConfig[String] with EchoClientConfig[String] with FiniteDurationLifecycleConfig
}
