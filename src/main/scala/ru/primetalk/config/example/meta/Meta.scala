package ru.primetalk.config.example.meta

import cats.{Applicative, Functor}
import cats.data.Reader
import cats.effect._
import eu.timepit.refined.W
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Interval.Closed
import org.http4s.Uri
import org.http4s.Uri.Authority
import shapeless.Nat._0

import scala.language.higherKinds
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

trait Protocols {
  sealed trait Protocol

  sealed trait HttpUrlProtocol extends Protocol
  sealed trait HttpSimpleGetProtocol extends Protocol

  /** Json POST and Json response. */
  sealed trait JsonHttpRest[RequestMessage, ResponseMessage] extends HttpUrlProtocol

  /** Simple get url and text response.
    * The request message is simply appended to the url.
    */
  sealed trait SimpleHttpGetRest[ResponseMessage] extends HttpSimpleGetProtocol
}

trait AddressResolving extends Protocols {
  type NodeId

  /** Actual address of the other node. */
  case class NodeAddress[N <: NodeId](host: Uri.Host)

  trait AddressResolver[F[_]] {
    /** Resolves the address of the given node id.
      * If the other node's address is not yet known, the returned `F` will block and wait until it's available.
      * It might also fail in case of timeout or the other node's failure.
      */
    def resolve[N <: NodeId](nodeId: N): F[NodeAddress[N]]
  }

  def localHostResolver[F[_]: Applicative]: AddressResolver[F] = new AddressResolver[F]{
    override def resolve[N <: NodeId](nodeId: N): F[NodeAddress[N]] =
      Applicative[F].pure(NodeAddress(Uri.IPv4("127.0.0.1")))
  }

}

trait EndPoints extends AddressResolving {
  type PortNumber = Refined[Int, Closed[_0, W.`65535`.T]]

  case class Port[Protocol](portNumber: PortNumber)


  case class EndPoint[P](node: NodeId, port: Port[P])

  /** It's an endpoint for an http protocol with url. */
  case class HttpUrlEndPoint[P <: HttpUrlProtocol](endPoint: EndPoint[P], path: String)

  /** An endpoint for a simple get protocol. */
  case class HttpSimpleGetEndPoint[P <: HttpSimpleGetProtocol](endPoint: EndPoint[P], pathPrefix: String)

  implicit class AddressResolverOps[F[_]](resolver: AddressResolver[F]) {

    def toUri(p: HttpUrlEndPoint[_])(implicit F: Functor[F]): F[Uri] =
      F.map(resolver.resolve(p.endPoint.node)) { address =>
        new Uri(
          scheme = Some(Uri.Scheme.http),
          authority = Some(Authority(
            userInfo = None,
            host = address.host,
            port = Some(p.endPoint.port.portNumber.value)
          )),
          path = "/" + p.path
        )
      }

  }

  implicit class HttpUrlEndPointOps[P <: HttpUrlProtocol](p: HttpUrlEndPoint[P]) {
    def toUri[F[_]: Functor](resolver: AddressResolver[F]): F[Uri] =
      resolver.toUri(p)
  }

  implicit class HttpSimpleGetEndPointOps[P <: HttpSimpleGetProtocol](p: HttpSimpleGetEndPoint[P]) {
    def toUri[F[_]: Functor](resolver: AddressResolver[F])(pathSuffix: String): F[Uri] =
      Functor[F].map(resolver.resolve(p.endPoint.node)) { address =>
        new Uri(
          scheme = Some(Uri.Scheme.http),
          authority = Some(Authority(
            userInfo = None,
            host = address.host,
            port = Some(p.endPoint.port.portNumber.value)
          )),
          path = p.pathPrefix + pathSuffix
        )
      }
  }

}

trait Configs extends EndPoints {
  trait NodeConfig {
  }

  trait ServiceRoleConfig {
    def nodeId: NodeId
    protected def providedService[P <: HttpUrlProtocol](port: Port[P], pathPrefix: String): HttpUrlEndPoint[P] =
      HttpUrlEndPoint(EndPoint(nodeId, port), pathPrefix)
  }

  /** Manages the lifetime of a node.
    * The node will run for the configured lifetime and then exit.
    */
  trait LifecycleManagerConfig {
    def lifetime: FiniteDuration
  }

  def lifecycle[F[_]: Timer: Sync](config: LifecycleManagerConfig)(implicit timer: Timer[F]): F[ExitCode] =
    Sync[F].map(
      Timer[F].sleep(config.lifetime)
    )(_ => ExitCode.Success)
}

trait ResourceAcquiring extends Configs {
  /**
    * Some services might look like resources.
    * Some of the roles could be considered as a resource -
    *   we can acquire it (and it might fail), we can terminate it.
    * They cannot normally terminate for their own internal logic.
    *
    */
  type ResourceReader[F[_], Config, A] = Reader[Config, Resource[F, A]]

  implicit class ResourceReaderOps[F[_], C1](r: ResourceReader[F, C1, Unit]){
    def >>[C2, A](other: ResourceReader[F, C2, A]): ResourceReader[F, C1 with C2, A] =
      Reader(config => r.run(config).flatMap(_ => other.run(config)))
  }

  implicit class IoCancelTokenOps[F[_]: Functor, A](io: F[Fiber[F, A]]){
    def toResource: Resource[F, Fiber[F, A]] =
      Resource.make(io)(_.cancel)
  }

}
trait Roles extends ResourceAcquiring  {

  trait RoleImpl[F[_]] {
    type Config
    def resource(
      implicit
      timer: Timer[F],
      contextShift: ContextShift[F],
      resolver: AddressResolver[F],
      applicative: Applicative[F],
      ec: ExecutionContext
    ): ResourceReader[F, Config, Unit]
  }

  trait ZeroRoleImpl[F[_]] extends RoleImpl[F] {
    type Config <: Any
    def resource(
      implicit
      timer: Timer[F],
      contextShift: ContextShift[F],
      resolver: AddressResolver[F],
      applicative: Applicative[F],
      ec: ExecutionContext
    ): ResourceReader[F, Config, Unit] =
      Reader(_ => Resource.pure[F, Unit](()))
  }

}

trait Meta extends Roles
