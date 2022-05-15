package xyz.district.gisimporter.rosreestr

import xyz.district.gisimporter.fixture.FeatureFixture
import xyz.district.gisimporter.mock.{CoordinatesTransformerMock, HttpClientFacadeMock, ImageContourFinderMock}
import xyz.district.gisimporter.*
import xyz.district.gisimporter.model.*
import zio.{ULayer, ZIO}
import zio.internal.stacktracer.Tracer
import zio.mock.Expectation.value
import zio.nio.file.Path
import zio.test.Assertion.{anything, equalTo}
import zio.test.{ZIOSpecDefault, assert}

object RosreestrDistrictScannerSpec extends ZIOSpecDefault {
  private val cdn = CadastralDistrictNumber("10", "10", "044444")
  private val id = 1
  private val cn = CadastralNumber(cdn, s"$id")

  private val feature = FeatureFixture.landPlotFeature.copy(
    attrs = FeatureFixture.landPlotFeature.attrs.copy(
      cn = cn.formatted,
      id = cn.toCadastralId
    )
  )

  private val spriteSize = RosreestrDistrictScanner.expectedFeatureSpriteImageSize(feature)

  private val geometryPoints = Seq(Point(1, 2), Point(3, 4))

  def spec = suite("RosreestrDistrictScanner.scan")(
    test("should return cadastral objects") {
      val mocks: ULayer[HttpClientFacade with ImageContourFinder with CoordinatesTransformer] =
        HttpClientFacadeMock.Get.of[FeatureResponse](
          equalTo((RosreestrUrl.getFeature(cn, FeatureType.LandPlot), Map.empty)),
          value(FeatureResponse(feature))
        ) ++
          HttpClientFacadeMock
            .GetBinary(equalTo(RosreestrUrl.exportMapSprite(feature, spriteSize)), value(Path("output.json"))) ++
          ImageContourFinderMock.FindContours(
            equalTo(Path("output.json")),
            value(geometryPoints)
          ) ++ CoordinatesTransformerMock.Epsg3785toGps(equalTo(Coordinate(1.0005, 2.099)), value(GpsCoordinate(3, 4)))
          ++ CoordinatesTransformerMock.Epsg3785toGps(equalTo(Coordinate(1.0015, 2.098)), value(GpsCoordinate(5, 6)))

      for {
        scanResult <- ZIO
          .serviceWithZIO[DistrictScanner](_.scan(cdn, Seq(id), 2))
          .provide(
            RosreestrDistrictScanner.layer,
            mocks
          )
      } yield assert(scanResult)(
        equalTo(
          DistrictCadastralObjects(
            Seq.empty,
            Seq(
              LandPlot(
                feature.attrs.address,
                Seq(GpsCoordinate(3.0, 4.0), GpsCoordinate(5.0, 6.0)),
                cn
              )
            )
          )
        )
      )
    }
  )
}
