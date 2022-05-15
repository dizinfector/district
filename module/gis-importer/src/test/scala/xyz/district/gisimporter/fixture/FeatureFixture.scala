package xyz.district.gisimporter.fixture

import xyz.district.gisimporter.rosreestr.*

object FeatureFixture {
  val landPlotFeature = Feature(
    attrs = Attrs(
      date_create = None,
      address = "Some address",
      cn = "10:01:01234567:123",
      id = "10:1:1234567:123"
    ),
    extent = Extent(
      xmin = 1.0,
      xmax = 1.1,
      ymin = 2.0,
      ymax = 2.1
    ),
    `type` = FeatureType.LandPlot.id
  )
}
