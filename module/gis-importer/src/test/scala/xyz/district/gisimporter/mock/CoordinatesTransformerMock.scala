package xyz.district.gisimporter.mock

import io.circe.Decoder
import xyz.district.gisimporter.model.GpsCoordinate
import xyz.district.gisimporter.rosreestr.Coordinate
import xyz.district.gisimporter.CoordinatesTransformer
import zio.internal.stacktracer.Tracer
import zio.mock.{Mock, Proxy}
import zio.nio.file.Path
import zio.{EnvironmentTag, Tag, Task, URLayer, ZIO, ZLayer}

object CoordinatesTransformerMock extends Mock[CoordinatesTransformer] {
  object Epsg3785toGps extends Effect[Coordinate, Throwable, GpsCoordinate]

  val compose: URLayer[Proxy, CoordinatesTransformer] = {
    given trace: Tracer.instance.Type = Tracer.newTrace

    ZLayer.fromZIO(
      ZIO
        .service[Proxy]
        .map(proxy =>
          new CoordinatesTransformer {
            override def epsg3785toGps(point: Coordinate): ZIO[Any, Throwable, GpsCoordinate] =
              proxy(Epsg3785toGps, point)
          }
        )
    )
  }
}
