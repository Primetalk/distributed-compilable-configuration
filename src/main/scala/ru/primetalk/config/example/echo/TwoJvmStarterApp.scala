package ru.primetalk.config.example.echo

import cats.effect._

import scala.language.higherKinds
import scala.reflect.ClassTag
import scala.sys.process.Process
import scala.sys.process.ProcessBuilder

object TwoJvmStarterApp extends IOApp {

  def getProperty[F[_]: Sync](name: String): F[String] =
    Sync[F].delay(System.getProperty(name))

  val sep: String = System.getProperty("file.separator")
  val classpath: String = System.getProperty("java.class.path")
  val path: String = System.getProperty("java.home")+sep+"bin"+sep+"java"

  def spawn[T: ClassTag](args: String*): IO[Process] = IO {
    val className = implicitly[ClassTag[T]].runtimeClass.getCanonicalName.dropRight(1) // remove $ at the end of object name
    val processBuilder: ProcessBuilder = Process(path :: "-cp" :: classpath :: className :: args.toList)
    processBuilder.run()
  }

  override def run(args: List[String]): IO[ExitCode] = for {
    serverProcess <- spawn[TwoJvmSingleNodeApp.type]("NodeServer")
    clientProcess <- spawn[TwoJvmSingleNodeApp.type]("NodeClient")
    _ <- IO{ clientProcess.exitValue() }
    _ <- IO{ serverProcess.destroy() }
  } yield ExitCode.Success
}
