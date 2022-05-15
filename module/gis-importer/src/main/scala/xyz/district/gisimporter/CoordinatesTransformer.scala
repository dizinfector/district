package xyz.district.gisimporter

import io.circe.Decoder
import io.circe.generic.AutoDerivation
import org.locationtech.proj4j.{CRSFactory, CoordinateTransform, CoordinateTransformFactory, ProjCoordinate}
import xyz.district.gisimporter.model.GpsCoordinate
import xyz.district.gisimporter.rosreestr.*
import xyz.district.gisimporter.rosreestr.{Feature, FeatureResponse}
import zio.stream.ZStream
import zio.{RIO, Task, TaskLayer, URIO, URLayer, ZIO, ZLayer}

/**
 * Трансформер координат
 */
trait CoordinatesTransformer {

  /**
   * Перевести координаты из формата EPSG 3785 в формат WGS 84
   *
   * @param point координата EPSG 3785
   * @return
   */
  def epsg3785toGps(point: Coordinate): ZIO[Any, Throwable, GpsCoordinate]
}

object CoordinatesTransformer {

  /**
   * Перевести координаты из формата EPSG 3785 в формат WGS 84
   *
   * @param point координата EPSG 3785
   * @return
   */
  def epsg3785toGps(point: Coordinate): RIO[CoordinatesTransformer, GpsCoordinate] =
    ZIO.serviceWithZIO[CoordinatesTransformer](_.epsg3785toGps(point))
}

case class CoordinatesTransformerImpl(coordinateTransform: CoordinateTransform) extends CoordinatesTransformer {

  /**
   * @inheritdoc
   */
  def epsg3785toGps(point: Coordinate): ZIO[Any, Throwable, GpsCoordinate] = {
    ZIO.attempt {
      val result = new ProjCoordinate()

      coordinateTransform.transform(
        new ProjCoordinate(point.x, point.y),
        result
      )

      GpsCoordinate(result.y, result.x)
    }
  }
}

object CoordinatesTransformerImpl {

  val layer: TaskLayer[CoordinatesTransformer] =
    ZLayer {
      for {
        coordinateTransformation <- ZIO.attempt {
          val crsFactory = new CRSFactory()
          val wgs84 = crsFactory.createFromName("epsg:4326") // другое название WGS 84
          val epsg3785 = crsFactory.createFromName("epsg:3785")

          val ctFactory = new CoordinateTransformFactory()

          ctFactory.createTransform(epsg3785, wgs84)
        }
      } yield CoordinatesTransformerImpl(coordinateTransformation)
    }
}
