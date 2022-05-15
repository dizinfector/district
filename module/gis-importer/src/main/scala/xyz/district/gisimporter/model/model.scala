package xyz.district.gisimporter.model

import xyz.district.gisimporter.*

import scala.util.{Failure, Success, Try}

/**
 * GPS координата
 * @param lat широта, например 62.068606
 * @param lon долгота, например 35.232464
 */
case class GpsCoordinate(lat: Double, lon: Double)

/**
 * Здание
 *
 * @param address адрес в произвольном формате
 * @param geometry координаты углов
 * @param cadastralNumber кадастровый номер
 */
case class Building(address: String, geometry: Seq[GpsCoordinate], cadastralNumber: CadastralNumber)

/**
 * Земельный участок
 *
 * @param address адрес в произвольном формате
 * @param geometry координаты углов
 * @param cadastralNumber кадастровый номер
 */
case class LandPlot(address: String, geometry: Seq[GpsCoordinate], cadastralNumber: CadastralNumber)

/**
 * Результат сканирования
 *
 * @param buildings здания
 * @param landPlots участки
 */
case class DistrictCadastralObjects(buildings: Seq[Building], landPlots: Seq[LandPlot])
