package ru.primetalk.config.example.echo


import cats.effect.Effect
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl

import scala.language.higherKinds
import scala.util.Try

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

  /** Create an implementation of service based on a function. */
  def simpleHttpGetService[F[_]: Effect, A: FromString, B: ToString](f: A => B): HttpRoutes[F] = {
    val dsl = Http4sDsl[F]
    import dsl._
    HttpRoutes.of[F]{
      case GET -> Root / message =>
        val inputEither: Either[Throwable, A] = implicitly[FromString[A]].fromString(message)
        inputEither.fold(
          t =>
            InternalServerError(t.getMessage),
          input => {
            Try(f(input)).toEither.fold(
              t => InternalServerError(t.getMessage),
              output =>
                Ok(implicitly[ToString[B]].convertToString(output))
            )
          }
        )
    }
  }


}
