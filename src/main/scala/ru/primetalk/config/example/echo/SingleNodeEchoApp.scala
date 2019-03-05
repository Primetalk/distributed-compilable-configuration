package ru.primetalk.config.example.echo

import cats.effect._
import cats.implicits._
import api._
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt
import scala.language.higherKinds

object SingleNodeEchoApp extends IOApp {

  val config:
    EchoConfig[String]
      with EchoClientConfig[String]
      with LifecycleManagerConfig
    = SingleNodeConfig

  override def run(args: List[String]): IO[ExitCode] = {
    implicit val resolver: AddressResolver[IO] = localHostResolver
    implicit val ec: ExecutionContext = ExecutionContext.global

    val allRoles = SingleNodeImpl.resource
    val allRolesResource: Resource[IO, Unit] = allRoles(config)
    allRolesResource.use( _ => lifecycle(config))
  }
}
