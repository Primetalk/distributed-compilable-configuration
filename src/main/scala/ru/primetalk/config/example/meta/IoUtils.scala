package ru.primetalk.config.example.meta

import cats.effect._

import scala.language.higherKinds
import cats.implicits._

import scala.concurrent.duration.FiniteDuration
import scala.reflect.ClassTag
import scala.sys.process.Process

object IoUtils {
  def runForeverContinuously[A](io: IO[A]): IO[Nothing] =
    io >>
      IO.cancelBoundary *>
        runForeverContinuously(io)

  def runForeverPeriodically[A](io: IO[A], pollInterval: FiniteDuration)(implicit timerIO: Timer[IO]): IO[Nothing] =
    io >>
      IO.cancelBoundary *>
        timerIO.sleep(pollInterval) *>
        runForeverPeriodically(io, pollInterval)

  def getProperty[F[_]: Sync](name: String): F[String] =
    Sync[F].delay(System.getProperty(name))

  def spawn[F[_]: Sync, T: ClassTag](args: String*): F[Process] =
    for {
      classpath <- getProperty("java.class.path")
      sep <- getProperty("file.separator")
      javaHome <- getProperty("java.home")
      path = javaHome + sep + "bin" + sep + "java"
      className = implicitly[ClassTag[T]].runtimeClass.getCanonicalName.dropRight(1)
      processBuilder = Process(path :: "-cp" :: classpath :: className :: args.toList)
      p <- Sync[F].delay{ processBuilder.run() }
    } yield p

  def spawnIO[T: ClassTag](args: String*): IO[Process] =
    spawn[IO,T](args : _ *)

  def destroyProcess(process: Process): IO[Unit] =
    IO{ process.destroy() }

  def waitForCompletion(process: Process): IO[ExitCode] =
    IO{
      val exitCode = process.exitValue()
      if(exitCode == 0) ExitCode.Success else ExitCode(exitCode)
    }

}
