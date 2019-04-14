package ru.primetalk.config.example

import ru.primetalk.config.example.meta.Meta

/** This API introduces two nodes */
package object api extends Meta {

  sealed trait NodeIdImpl
  case object Backend extends NodeIdImpl
  case object Frontend extends NodeIdImpl

  case object Singleton extends NodeIdImpl

  case class Ping(r: String)
  /** Depends on `Ping`. */
  case class Pong(request: Ping, ans: String)

  type PingPongProtocol = SimpleHttpGetRest[Ping, Pong]

  trait PingPongConfig extends ServiceConfig {
    def pingPongPortNumber: PortNumber //PortWithPrefix[PingPongProtocol]
    def pingPongPort: PortWithPrefix[PingPongProtocol] = PortWithPrefix(pingPongPortNumber, "ping")
    def pingPongService: HttpSimpleGetEndPoint[NodeId, PingPongProtocol] = providedSimpleService(pingPongPort)
    def pongDependency: HttpSimpleGetEndPoint[NodeId, PingPongProtocol]
  }

  type MyRoleRequest = Ping
  type MyRoleResponse = Pong

  type MyServiceProtocol = JsonHttpRest[MyRoleRequest, MyRoleResponse]

  type Dependency1Request = Ping
  type Dependency1Response = Pong

  type OtherServiceProtocol = JsonHttpRest[Dependency1Request, Dependency1Response]

  trait MyConfig extends ServiceConfig {
    def servicePort1: MyServiceProtocol

    def dependency1: TcpEndPoint[NodeId, OtherServiceProtocol]
  }

  trait Node[NodeId] {
    def nodeId: NodeId
  }

}
