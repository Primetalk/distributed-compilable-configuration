package ru.primetalk.config.example.echo

import cats.effect._
import ru.primetalk.config.example.meta.IoUtils

import scala.language.higherKinds

object TwoJvmStarterApp extends IOApp {

  override def run(args: List[String]): IO[ExitCode] =
    TwoJvmSingleNodeApp.spawnServer.bracket(_ =>
      TwoJvmSingleNodeApp.spawnClient.bracket(
        IoUtils.waitForCompletion
      )(IoUtils.destroyProcess)
    )(IoUtils.destroyProcess)

}
