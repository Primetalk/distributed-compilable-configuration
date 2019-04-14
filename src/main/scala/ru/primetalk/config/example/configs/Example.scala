package ru.primetalk.config.example.configs

import cats.effect._
import ru.primetalk.config.example.api._

import scala.language.higherKinds

object Example extends IOApp {
  case object BackendNodeConfig extends MyConfig {

    override type NodeId = NodeIdImpl
    override def nodeId: Backend.type = Backend

    override def servicePort1: MyServiceProtocol = ???

    override def dependency1: TcpEndPoint[NodeId, OtherServiceProtocol] = ???

  }

  case object BackendNode extends Node[Backend.type] {

    override def nodeId: Backend.type = Backend

  }

//  object MyRoleStarter extends RoleStarter[IO, MyRoleConfig] {
//    override def apply(resolver: AddressResolver[F])(ready: Ready[F]): F[CancelToken[F]] =
//      Sync[F].delay(ready)
//  }
//
//  override def run(args: List[String]): IO[ExitCode] = IO {
//    ExitCode.Success
//  }
  override def run(args: List[String]): IO[ExitCode] = IO{
    ExitCode.Success
  }
}
