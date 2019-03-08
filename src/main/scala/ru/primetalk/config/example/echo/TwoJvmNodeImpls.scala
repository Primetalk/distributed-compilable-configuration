package ru.primetalk.config.example.echo

import cats.effect.IO
import ru.primetalk.config.example.echo.api.{EchoClientConfig, EchoConfig}
import api._

object TwoJvmNodeImpls {

  object TwoJvmNodeServerImpl extends ZeroServiceImpl[IO] with EchoServiceService with SigIntLifecycleServiceImpl {
    type Config = EchoConfig[String] with SigTermLifecycleConfig
  }

  object TwoJvmNodeClientImpl extends ZeroServiceImpl[IO] with EchoClientService with FiniteDurationLifecycleServiceImpl {
    type Config = EchoClientConfig[String] with FiniteDurationLifecycleConfig
  }

}
