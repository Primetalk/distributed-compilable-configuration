package ru.primetalk.config.example.echo

import cats.effect.IO
import ru.primetalk.config.example.echo.api.{EchoClientConfig, EchoConfig}
import api._

object TwoJvmNodeImpls {

  object TwoJvmNodeServerImpl extends ZeroRoleImpl[IO] with EchoServiceRole with SigIntLifecycleRoleImpl {
    type Config = EchoConfig[String] with SigTermLifecycleConfig
  }

  object TwoJvmNodeClientImpl extends ZeroRoleImpl[IO] with EchoClientRole with FiniteDurationLifecycleRoleImpl {
    type Config = EchoClientConfig[String] with FiniteDurationLifecycleConfig
  }

}
