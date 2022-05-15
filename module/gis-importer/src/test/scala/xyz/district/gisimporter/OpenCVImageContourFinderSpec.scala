package xyz.district.gisimporter

import zio.ZIO
import zio.nio.file.Path
import zio.test.Assertion.equalTo
import zio.test.{ZIOSpecDefault, assert}

object OpenCVImageContourFinderSpec extends ZIOSpecDefault {
  def spec = suite("OpenCVImageContourFinder.findContours")(
    test("should return image coordinates") {
      val imagePath = Path(getClass.getClassLoader.getResource("land-plot-example.png").toURI)
      for {
        contours <- ZIO
          .serviceWithZIO[ImageContourFinder](_.findContours(imagePath))
          .provide(
            OpenCVImageContourFinder.layer
          )
      } yield assert(contours)(
        equalTo(
          List(
            Point(25.0, 24.0),
            Point(126.0, 25.0),
            Point(126.0, 73.0),
            Point(175.0, 74.0),
            Point(174.0, 175.0),
            Point(73.0, 174.0),
            Point(73.0, 126.0),
            Point(24.0, 125.0)
          )
        )
      )
    }
  )
}
