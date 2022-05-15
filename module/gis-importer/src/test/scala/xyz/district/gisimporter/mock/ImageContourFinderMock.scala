package xyz.district.gisimporter.mock

import io.circe.Decoder
import xyz.district.gisimporter.{ImageContourFinder, Point}
import zio.internal.stacktracer.Tracer
import zio.mock.{Mock, Proxy}
import zio.nio.file.Path
import zio.{EnvironmentTag, Tag, Task, URLayer, ZIO, ZLayer}

object ImageContourFinderMock extends Mock[ImageContourFinder] {

  object FindContours extends Effect[Path, Throwable, Seq[Point]]

  val compose: URLayer[Proxy, ImageContourFinder] = {
    given trace: zio.ZTraceElement = Tracer.newTrace

    ZLayer.fromZIO(
      ZIO
        .service[Proxy]
        .map(proxy =>
          new ImageContourFinder {
            def findContours(imagePath: Path): ZIO[Any, Throwable, Seq[Point]] = proxy(FindContours, imagePath)
          }
        )
    )
  }
}
