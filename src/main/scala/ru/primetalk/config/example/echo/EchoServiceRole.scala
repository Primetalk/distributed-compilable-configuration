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

object SimpleHttpGetServiceImpl {
  trait FromString[A] {
    def fromString(str: String): Either[Throwable, A]
  }

  trait ToString[A] {
    def convertToString(a: A): String
  }

  implicit object FromStringString extends FromString[String] {
    override def fromString(a: String): Either[Throwable, String] = Right(a)
  }
  implicit object ToStringString extends ToString[String] {
    override def convertToString(a: String): String = a
  }

  def simpleHttpGetService[F[_]: Effect, A: FromString, B: ToString](f: A => B): HttpRoutes[F] = {
    val dsl = Http4sDsl[F]
    import dsl._
    HttpRoutes.of[F]{
      case GET -> Root / message =>
        val inputEither: Either[Throwable, A] = implicitly[FromString[A]].fromString(message)
        inputEither.fold(
          t =>
            InternalServerError(t.getMessage),
          input =>
            Ok(implicitly[ToString[B]].convertToString(f(input)))
        )
    }
  }


}
trait EchoServiceRole extends RoleImpl[IO] {

  import SimpleHttpGetServiceImpl._
  def echoFunction[A](input: A): A = input

  private def echoServiceAcquirer3[A: FromString : ToString, C<:EchoConfig[A]](implicit timerIO: Timer[IO], contextShift: ContextShift[IO], resolver: AddressResolver[IO]): ResourceReader[IO, C, Unit] =
    Reader(config => {
      val httpApp =
        Router[IO]("/" -> simpleHttpGetService(echoFunction[A])).orNotFound
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
    super.resource >>[Config, Unit] echoServiceAcquirer3[String, Config]


}
