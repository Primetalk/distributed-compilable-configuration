package ru.primetalk.config.example.echo

import cats.Applicative
import cats.effect.{ContextShift, IO, Timer}
import api._
import cats.data.Reader
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.implicits._

import scala.concurrent.ExecutionContext
import scala.language.higherKinds

trait EchoServiceService extends ServiceImpl[IO] {

  import SimpleHttpGetServiceImpl._
  def echoFunction[A](input: A): A = input

  private def echoServiceAcquirer3[A: FromString : ToString, C<:EchoConfig[A]](implicit timerIO: Timer[IO], contextShift: ContextShift[IO], resolver: AddressResolver[IO]): ResourceReader[IO, C, Unit] =
    Reader(config => {
      val httpApp =
        Router[IO]("/" + config.echoService.port.pathPrefix + "/" ->
          simpleHttpGetService(echoFunction[A])).orNotFound
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

  abstract override def resource(implicit
    resolver: AddressResolver[IO],
    timer: Timer[IO],
    contextShift: ContextShift[IO],
    applicative: Applicative[IO],
    ec: ExecutionContext): ResourceReader[IO, Config, Unit] =
    super.resource >>[Config, Unit] echoServiceAcquirer3[String, Config]


}
