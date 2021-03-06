package ru.primetalk.config.example.echo

import cats.implicits._
import cats.effect.{ExitCode, IO, IOApp}
import ru.primetalk.config.example.echo.TwoJvmNodeImpls.{TwoJvmNodeClientImpl, TwoJvmNodeServerImpl}
import ru.primetalk.config.example.echo.api.localHostResolver

import scala.concurrent.ExecutionContext
import api._
import ru.primetalk.config.example.meta.IoUtils

import scala.sys.process.Process

/** This is an application that will be run in each sub process.
  * The actual implementation is determined by single argument.
  *
  * One might also run this application manually in two separate JVMs.
  */
object TwoJvmSingleNodeApp extends IOApp {
  implicit val resolver: AddressResolver[IO] = localHostResolver
  implicit val ec: ExecutionContext = ExecutionContext.global

  def spawnNode(nodeId: String): IO[Process] =
    IoUtils.spawnIO[TwoJvmSingleNodeApp.type](nodeId)

  def spawnServer: IO[Process] = spawnNode("NodeServer")

  def spawnClient: IO[Process] = spawnNode("NodeClient")

  override def run(args: List[String]): IO[ExitCode] = args match {
    case List("NodeServer") =>
      TwoJvmNodeServerImpl.run(TwoJvmConfig.NodeServerConfig)
    case List("NodeClient") =>
      TwoJvmNodeClientImpl.run(TwoJvmConfig.NodeClientConfig)
    case _ =>
      IO{ println("Invalid arguments: " + args.mkString("'", " ", "'")) } >>
        IO{ println("Expected either NodeServer or NodeClient") } >>
        IO{ ExitCode.Error }
  }
}
