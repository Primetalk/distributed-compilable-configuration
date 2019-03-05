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

trait EchoServiceRole extends RoleImpl[IO] {

  private def echoService[F[_]: Effect]: HttpRoutes[F] = {
    val dsl = Http4sDsl[F]
    import dsl._
    HttpRoutes.of[F]{
      case GET -> Root / message =>
        println("Got request " + message + Thread.currentThread().getName)
        Ok(message)
    }
  }

  private def echoServiceAcquirer3[C<:EchoConfig[String]](implicit timerIO: Timer[IO], contextShift: ContextShift[IO], resolver: AddressResolver[IO]): ResourceReader[IO, C, Unit] =
    Reader(config => {
      val httpApp =
        Router[IO]("/" -> echoService).orNotFound
      val serverBuilder =
        BlazeServerBuilder[IO]
          .bindHttp(config.echoPort.portNumber.value, "0.0.0.0")
          .withHttpApp(httpApp)
      serverBuilder
        .resource
        .map(_ => ())
    }
    )

  override type Config <: EchoConfig[String]

  abstract override def resource(implicit timer: Timer[IO], contextShift: ContextShift[IO],
    resolver: AddressResolver[IO],
    applicative: Applicative[IO],
    ec: ExecutionContext): ResourceReader[IO, Config, Unit] =
    super.resource >>[Config, Unit] echoServiceAcquirer3[Config]


}
