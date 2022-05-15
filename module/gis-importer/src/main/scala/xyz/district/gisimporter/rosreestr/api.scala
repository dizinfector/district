package xyz.district.gisimporter.rosreestr

/**
 * Атрибуты кадастрового объекта
 *
 * @param date_create дата учета
 * @param address адрес в произвольном формате
 * @param cn кадастровый номер
 * @param id кадастровый идентификатор (тот же номер только без ведущих нулей)
 */
case class Attrs(date_create: Option[String], address: String, cn: String, id: String)

/**
 * Границы области в координатах EPSG 3785
 */
case class Extent(xmax: Double, ymax: Double, xmin: Double, ymin: Double)

/**
 * Кадастровый объект
 *
 * @param attrs атрибуты
 * @param extent границы области
 * @param `type` тип @see [[FeatureType]]
 */
case class Feature(attrs: Attrs, extent: Extent, `type`: Int) {
  def enumType: FeatureType = FeatureType.values
    .find(_.id == `type`)
    .getOrElse(throw new Exception(s"unknown feature type for feature $this"))
}

/**
 * Ответ на запрос кадастрового объекта
 *
 * @param feature кадастровый объект
 */
case class FeatureResponse(feature: Feature)

/**
 * Тип кадастрового объекта
 */
enum FeatureType(val id: Int):
  // земельный участок
  case LandPlot extends FeatureType(1)
  // здание
  case Building extends FeatureType(5)
