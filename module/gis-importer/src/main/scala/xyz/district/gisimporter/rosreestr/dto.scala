package xyz.district.gisimporter.rosreestr

/**
 * Размер спрайта кадастрового объекта
 */
case class SpriteImageSize(width: Int, height: Int)

/**
 * Координата EPSG 3785
 */
case class Coordinate(x: Double, y: Double)

/**
 * Геометрия кадастрового объекта
 */
case class Geometry(coordinates: Seq[Coordinate])
