package ru.primetalk.config.example.meta

import cats.effect._

import scala.language.higherKinds
import cats.implicits._

import scala.concurrent.duration.FiniteDuration

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

}
