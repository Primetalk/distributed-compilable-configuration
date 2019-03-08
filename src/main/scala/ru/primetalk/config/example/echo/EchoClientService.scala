package ru.primetalk.config.example.echo

import cats.Applicative
import cats.effect._
import api._
import cats.data.Reader
import org.http4s.Uri
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder

import scala.concurrent.ExecutionContext
import scala.language.higherKinds
import ru.primetalk.config.example.meta.IoUtils._

trait EchoClientService extends ServiceImpl[IO] {

  private def singleRoundOfRequestResponse(client: Client[IO], uri: Uri): IO[ExitCode] =
    for {
      _ <- IO{ println("Request:  " + uri) }
      response <- client.expect[String](uri)
      _ <- IO{ println("Response: " + response) }
    } yield ExitCode.Success

  private def echoClientStarter[C<:EchoClientConfig[String]](implicit ec: ExecutionContext,
    timerIO: Timer[IO],
    contextShift: ContextShift[IO],
    resolver: AddressResolver[IO]
  ): ResourceReader[IO, C, Unit] =
    Reader(config => {
      for {
        uri <- config.echoServiceDependency.toUri(resolver)(config.testMessage)
        _   <-
          BlazeClientBuilder[IO](ec).resource
            .use { client =>
              runForeverPeriodically(
                singleRoundOfRequestResponse(client, uri),
                config.pollInterval
              )
            }
      } yield ExitCode.Success
    }.start.toResource.map(_ => ())
    )

  override type Config <: EchoClientConfig[String]

  abstract override def resource(implicit timer: Timer[IO], contextShift: ContextShift[IO],
    resolver: AddressResolver[IO],
    applicative: Applicative[IO],
    ec: ExecutionContext): ResourceReader[IO, Config, Unit] =
    super.resource >>[Config, Unit] echoClientStarter[Config]

}
