package ru.primetalk.config.example.echo

import cats.effect._
import api._
import scala.concurrent.ExecutionContext

object SingleNodeEchoApp extends IOApp {

  val config:
    EchoConfig[String]
      with EchoClientConfig[String]
      with FiniteDurationLifecycleConfig
    = SingleNodeConfig

  implicit val resolver: AddressResolver[IO] = localHostResolver
  implicit val ec: ExecutionContext = ExecutionContext.global

  override def run(args: List[String]): IO[ExitCode] =
    SingleNodeImpl.run(config)

}
