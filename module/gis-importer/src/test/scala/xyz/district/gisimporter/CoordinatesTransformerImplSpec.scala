package xyz.district.gisimporter

import xyz.district.gisimporter.model.GpsCoordinate
import xyz.district.gisimporter.rosreestr.Coordinate
import zio.ZIO
import zio.test.Assertion.equalTo
import zio.test.{ZIOSpecDefault, assert}

object CoordinatesTransformerImplSpec extends ZIOSpecDefault {
  def spec = suite("CoordinatesTransformerImpl.epsg3785toGps")(
    test("should transform coordinates") {
      val point = Coordinate(3825449.2788586714, 8811665.862767609)
      for {
        gpsCoordinates <- ZIO
          .serviceWithZIO[CoordinatesTransformer](_.epsg3785toGps(point))
          .provide(
            CoordinatesTransformerImpl.layer
          )
      } yield assert(gpsCoordinates)(
        equalTo(GpsCoordinate(61.79911484205888, 34.364595558227464))
      )
    }
  )
}
