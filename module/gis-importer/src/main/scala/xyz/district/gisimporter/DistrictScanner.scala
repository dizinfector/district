package xyz.district.gisimporter

import xyz.district.gisimporter.model.DistrictCadastralObjects
import zio.{RIO, ZIO}

/**
 * Сканер кадастрового "района"
 */
trait DistrictScanner {

  /**
   * Сканирует кадастровый район
   *
   * @param districtNumber номер кадастрового район
   * @param cadastralObjectIds набор последних чисел кадастрового номера
   * @param parallelism степень параллелизма сканирования
   * @param trace трассировщик ZIO
   * @return объект с участками и зданиями
   */
  def scan(
    districtNumber: CadastralDistrictNumber,
    cadastralObjectIds: Seq[Int],
    parallelism: Int
  )(implicit trace: zio.ZTraceElement): ZIO[Any, Throwable, DistrictCadastralObjects]
}

object DistrictScanner {

  /**
   * Сканирует кадастровый район
   *
   * @param districtNumber номер кадастрового район
   * @param cadastralObjectIds набор последних чисел кадастрового номера
   * @param parallelism степень параллелизма сканирования
   * @param trace трассировщик ZIO
   * @return объект с участками и зданиями
   */
  def scan(
    districtNumber: CadastralDistrictNumber,
    cadastralObjectIds: Seq[Int],
    parallelism: Int
  )(implicit trace: zio.ZTraceElement): RIO[DistrictScanner, DistrictCadastralObjects] =
    ZIO.serviceWithZIO[DistrictScanner](_.scan(districtNumber, cadastralObjectIds, parallelism))
}
