package ru.primetalk.config.example.meta

import cats.{Applicative, Functor}
import cats.data.Reader
import cats.effect._
import eu.timepit.refined.W
import eu.timepit.refined.api.{Inference, RefType, Refined, Validate}
import eu.timepit.refined.macros.RefineMacro
import eu.timepit.refined.numeric.Interval.Closed
import eu.timepit.refined.string.MatchesRegex
import org.http4s.Uri
import org.http4s.Uri.Authority
import shapeless.Nat._0

import scala.language.higherKinds
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration
import scala.language.experimental.macros

trait Protocols {
  sealed trait Protocol

  sealed trait HttpUrlProtocol extends Protocol
  sealed trait HttpSimpleGetProtocol extends Protocol

  /** Json POST and Json response. */
  sealed trait JsonHttpRest[RequestMessage, ResponseMessage] extends HttpUrlProtocol

  /** Simple get url and text response.
    * The request message is simply appended to the url as a string
    * and the response will just contain string (`text/plain`).
    */
  sealed trait SimpleHttpGetRest[RequestMessage, ResponseMessage] extends HttpSimpleGetProtocol
}

trait AddressResolving extends Protocols {

  /** Actual address of the other node. */
  case class NodeAddress[NodeId](host: Uri.Host)

  trait AddressResolver[F[_]] {
    /** Resolves the address of the given node id.
      * If the other node's address is not yet known, the returned `F` will block and wait until it's available.
      * It might also fail in case of timeout or the other node's failure.
      */
    def resolve[NodeId](nodeId: NodeId): F[NodeAddress[NodeId]]
  }

  def localHostResolver[F[_]: Applicative]: AddressResolver[F] = new AddressResolver[F]{
    override def resolve[NodeId](nodeId: NodeId): F[NodeAddress[NodeId]] =
      Applicative[F].pure(NodeAddress(Uri.IPv4("127.0.0.1")))
  }

}

trait EndPoints extends AddressResolving {
  /** Copy of eu.timepit.refined.auto#autoRefineV for convenience. So that
    * in config file we don't have to import additional packages. Just `import api._`.
    */
  implicit def autoRefineVRef[T, P](t: T)(
    implicit rt: RefType[Refined],
    v: Validate[T, P]
  ): Refined[T, P] = macro RefineMacro.impl[Refined, T, P]

  type PortNumber = Refined[Int, Closed[_0, W.`65535`.T]]

  type UrlPathPrefix = Refined[String, MatchesRegex[W.`"[a-zA-Z_0-9/]*"`.T]]

  type UrlPathElement = Refined[String, MatchesRegex[W.`"[a-zA-Z_0-9]*"`.T]]

  implicit def inferPrefixFromElement: Inference[MatchesRegex[W.`"[a-zA-Z_0-9]*"`.T], MatchesRegex[W.`"[a-zA-Z_0-9/]*"`.T]] =
    Inference(isValid = true, "Path element is also a path prefix")

  case class TcpPort[Protocol](portNumber: PortNumber)

  case class PortWithPrefix[Protocol](portNumber: PortNumber, pathPrefix: UrlPathPrefix)

  case class TcpEndPoint[NodeId, P](node: NodeId, port: TcpPort[P])

  /** It's an endpoint for an http protocol with url. */
  case class HttpUrlEndPoint[NodeId, P <: HttpUrlProtocol](endPoint: TcpEndPoint[NodeId, P], path: String)

  /** An endpoint for a simple get protocol. */
  case class HttpSimpleGetEndPoint[NodeId, P <: HttpSimpleGetProtocol](node: NodeId, port: PortWithPrefix[P])

  implicit class AddressResolverOps[F[_]](resolver: AddressResolver[F]) {

    def toUri(p: HttpUrlEndPoint[_, _])(implicit F: Functor[F]): F[Uri] =
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

  implicit class HttpUrlEndPointOps[NodeId, P <: HttpUrlProtocol](p: HttpUrlEndPoint[NodeId, P]) {
    def toUri[F[_]: Functor](resolver: AddressResolver[F]): F[Uri] =
      resolver.toUri(p)
  }

  implicit class HttpSimpleGetEndPointOps[NodeId, P <: HttpSimpleGetProtocol](p: HttpSimpleGetEndPoint[NodeId, P]) {
    def toUri[F[_]: Functor](resolver: AddressResolver[F])(pathSuffix: UrlPathElement): F[Uri] =
      Functor[F].map(resolver.resolve(p.node)) { address =>
        new Uri(
          scheme = Some(Uri.Scheme.http),
          authority = Some(Authority(
            userInfo = None,
            host = address.host,
            port = Some(p.port.portNumber.value)
          )),
          path = p.port.pathPrefix + pathSuffix.value
        )
      }
  }

}

trait Configs extends EndPoints {
  trait NodeConfig {
  }

  trait ServiceConfig {
    type NodeId
    def nodeId: NodeId
    protected def providedSimpleService[P <: HttpSimpleGetProtocol](port: PortWithPrefix[P]): HttpSimpleGetEndPoint[NodeId, P] =
      HttpSimpleGetEndPoint(nodeId, port)
  }

  /** Manages the lifetime of a node.
    * The node will run for the configured lifetime and then exit.
    */
  trait FiniteDurationLifecycleConfig {
    def lifetime: FiniteDuration
  }

  def finiteLifecycle[F[_]: Timer: Sync](config: FiniteDurationLifecycleConfig): F[ExitCode] =
    Sync[F].map(
      Timer[F].sleep(config.lifetime)
    )(_ => ExitCode.Success)

  /** Manages the lifetime of a node.
    * The node will run until SIGTERM and then normally exit.
    */
  trait SigTermLifecycleConfig {
    // no configuration is actually needed
  }

  def lifecycleSigTerm[F[_]: Async](config: SigTermLifecycleConfig): F[ExitCode] =
    Sync[F].map(
      Async[F].async[Nothing](_ => ()) // ignoring callback. This async computation will never finish.
                                       // Hence `Nothing` result type
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
trait Services extends ResourceAcquiring  {

  trait ServiceImpl[F[_]] {
    type Config
    def resource(
      implicit
      resolver: AddressResolver[F],
      timer: Timer[F],
      contextShift: ContextShift[F],
      applicative: Applicative[F],
      ec: ExecutionContext
    ): ResourceReader[F, Config, Unit]
  }

  trait ZeroServiceImpl[F[_]] extends ServiceImpl[F] {
    type Config <: Any
    def resource(
      implicit
      resolver: AddressResolver[F],
      timer: Timer[F],
      contextShift: ContextShift[F],
      applicative: Applicative[F],
      ec: ExecutionContext
    ): ResourceReader[F, Config, Unit] =
      Reader(_ => Resource.pure[F, Unit](()))
  }

  trait LifecycleServiceImpl[F[_]] extends ServiceImpl[F] {

    def lifecycle(config: Config)(implicit timer: Timer[F], async: Async[F]): F[ExitCode]

    def run(config: Config)(implicit
      resolver: AddressResolver[F],
      timer: Timer[F],
      contextShift: ContextShift[F],
      async: Async[F],
      applicative: Applicative[F],
      ec: ExecutionContext
    ): F[ExitCode] = {
      val allServicesAsResourceReader = resource
      val allServicesResource: Resource[F, Unit] = allServicesAsResourceReader(config)
      allServicesResource.use( _ => lifecycle(config))
    }

  }

  trait FiniteDurationLifecycleServiceImpl extends LifecycleServiceImpl[IO] {
    type Config <: FiniteDurationLifecycleConfig

    override def lifecycle(config: Config)(implicit timer: Timer[IO], async: Async[IO]): IO[ExitCode] =
      finiteLifecycle(config)

  }

  trait SigIntLifecycleServiceImpl extends LifecycleServiceImpl[IO] {
    type Config <: SigTermLifecycleConfig

    override def lifecycle(config: Config)(implicit timer: Timer[IO], async: Async[IO]): IO[ExitCode] =
      lifecycleSigTerm(config)

  }

}

trait Meta extends Services
