package xyz.district.gisimporter.rosreestr

import xyz.district.gisimporter.Point
import xyz.district.gisimporter.CadastralNumber
import xyz.district.gisimporter.rosreestr.FeatureType.*

object RosreestrUrl {
  // id слоя участков
  private val LandPlotLayerId = 8
  // id слоя зданий
  private val BuildingLayerId = 5
  // DPI спрайта
  private val SPRITE_DPI = 72
  // формат изображения спрайта
  private val SPRITE_FORMAT = "png32"
  // формат возвратщаемого типа
  private val RETURN_FORMAT = "image"
  // непонятно, что за параметр. Параметр нужный
  private val SR = 102100

  private def spriteLayer(featureType: FeatureType): Int = {
    featureType match
      case LandPlot => LandPlotLayerId
      case Building => BuildingLayerId
  }

  /**
   * Сформировать строковый URL для получения "спрайта" кадастрового объекта
   *
   * @param feature кадастровый объект
   * @param spriteSize размер "спрайта"
   * @return
   */
  def exportMapSprite(feature: Feature, spriteSize: SpriteImageSize): (String, Map[String, String]) = {
    val extent = feature.extent
    val layerIds = Seq(spriteLayer(feature.enumType))

    val bBox = Seq(
      extent.xmin,
      extent.ymin,
      extent.xmax,
      extent.ymax
    )

    val widthHeight = spriteSize
    val size = s"${widthHeight.width},${widthHeight.height}"
    val cadId = feature.attrs.id

    val layers = s"show:${layerIds.mkString(",")}"
    val layerDefs = layerIds.map(id => s""""$id":"id = '$cadId'"""").mkString("{", ",", "}")

    (
      "https://pkk.rosreestr.ru/arcgis/rest/services/PKK6/CadastreSelected/MapServer/export?",
      Map(
        "bbox" -> bBox.mkString(","),
        "bboxSR" -> SR.toString,
        "imageSR" -> SR.toString,
        "size" -> size,
        "format" -> SPRITE_FORMAT,
        "transparent" -> true.toString,
        "layers" -> layers,
        "layerDefs" -> layerDefs,
        "f" -> RETURN_FORMAT,
        "dpi" -> SPRITE_DPI.toString
      )
    )
  }

  /**
   * Сформировать строковый URL для получения данных кадастрового объекта
   *
   * @param cadastralNumber кадастровый номер
   * @param featureType тип кадастрового объекта
   * @return
   */
  def getFeature(cadastralNumber: CadastralNumber, featureType: FeatureType): String = {
    s"https://pkk.rosreestr.ru/api/features/${featureType.id}/${cadastralNumber.toCadastralId}"
  }
}
