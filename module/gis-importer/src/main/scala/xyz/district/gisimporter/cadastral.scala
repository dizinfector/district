package xyz.district.gisimporter

import scala.util.{Failure, Try}

class CadastralDistrictNumberParseException(cdn: String) extends Exception(s""""$cdn" has wrong format""")

class CadastralNumberParseException(cn: String) extends Exception(s""""$cn" has wrong format""")

/**
 * Номер кадастрового "района"
 * Пример 10:01:0100119
 *
 * @param districtId округ
 * @param subdistrictId район
 * @param blockId квартал
 *
 * Все идентификаторы строковые так как числа могут быть с начальным нулем
 */
case class CadastralDistrictNumber(districtId: String, subdistrictId: String, blockId: String)

object CadastralDistrictNumber {
  private val cdnRegex = raw"(\d+):(\d+):(\d+)".r

  /**
   * Разобрать номер из строки
   *
   * @param cdn строковое представление номера, например 78:32:0001010
   * @return
   */
  def fromString(cdn: String): Try[CadastralDistrictNumber] = {
    cdn match {
      case cdnRegex(districtId, districtAreaId, areaId) =>
        Try(CadastralDistrictNumber(districtId, districtAreaId, areaId))
      case _ => Failure(new CadastralDistrictNumberParseException(cdn))
    }
  }
}

/**
 * Кадастровый номер здания/участка
 * Пример 10:1:100119:55
 *
 * @param districtNumber
 *   составной идентификатор
 * @param id
 *   номер
 */
case class CadastralNumber(districtNumber: CadastralDistrictNumber, id: String) {

  /**
   * Строковое представление кадастрового номера
   *
   * @return
   */
  def formatted: String = s"${districtNumber.districtId}:${districtNumber.subdistrictId}:${districtNumber.blockId}:$id"

  /**
   * Строковоый идентификатор номера. Тоже что и номер но без ведущих нулей
   */
  lazy val toCadastralId: String =
    s"${districtNumber.districtId.toInt}:" +
      s"${districtNumber.subdistrictId.toInt}:" +
      s"${districtNumber.blockId.toInt}:${id.toInt}"

  override def toString: String = formatted
}

object CadastralNumber {
  private val coRegex = raw"(\d+):(\d+):(\d+):(\d+)".r

  /**
   * Разобрать номер из строки
   *
   * @param cn строковое представление номера, например 78:32:0001010:22
   * @return
   */
  def fromString(cn: String): Try[CadastralNumber] = {
    cn match {
      case coRegex(districtId, districtAreaId, areaId, id) =>
        Try(CadastralNumber(CadastralDistrictNumber(districtId, districtAreaId, areaId), id))
      case _ => Failure(new Exception(CadastralNumberParseException(cn)))
    }
  }
}
