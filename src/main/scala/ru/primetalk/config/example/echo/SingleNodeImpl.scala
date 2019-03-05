package ru.primetalk.config.example.echo

import cats.Applicative
import cats.effect.{ContextShift, Effect, IO, Timer}
import api._
import cats.data.Reader
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.implicits._

import scala.concurrent.ExecutionContext
import scala.language.higherKinds

object SingleNodeImpl extends ZeroRoleImpl[IO]
  with EchoServiceRole
  with EchoClientRole
{
  type Config = EchoConfig[String] with EchoClientConfig[String]
}
