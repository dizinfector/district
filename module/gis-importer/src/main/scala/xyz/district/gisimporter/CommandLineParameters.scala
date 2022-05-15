package xyz.district.gisimporter

import java.io.File
import io.github.vigoo.clipp.*
import io.github.vigoo.clipp.parsers.*
import io.github.vigoo.clipp.syntax.*
import io.github.vigoo.clipp.zioapi.*
import xyz.district.gisimporter.CadastralDistrictNumber
import zio.nio.file.Path

import scala.util.Try

/**
 * Параметры командной строки приложения
 *
 * @param outputFilePath путь к файлу для записи результатов сканирования
 * @param cadastralDistrictNumber номер кадастрового "района"
 * @param cadastralObjectIds набор последних чисел кадастрового номера (XX:XX:XXXXXXX:<последнее число>)
 */
case class CommandLineParameters(
  outputFilePath: Path,
  cadastralDistrictNumber: CadastralDistrictNumber,
  cadastralObjectIds: Seq[Int]
)

/**
 * Спецификация для парсинга параметров командной строки
 */
object CommandLineParameters {
  private given pathParser: ParameterParser[Path] = new ParameterParser[Path] {
    override def parse(value: String): Either[String, Path] = {
      Try(Path(value)).toEither.left.map(_.getMessage)
    }

    override def example: Path = Path("file.json")
  }

  private given cdnParser: ParameterParser[CadastralDistrictNumber] = new ParameterParser[CadastralDistrictNumber] {
    override def parse(value: String): Either[String, CadastralDistrictNumber] = {
      CadastralDistrictNumber.fromString(value).toEither.left.map(_.getMessage)
    }

    override def example: CadastralDistrictNumber = CadastralDistrictNumber("10", "01", "0120124")
  }

  private given coIdsParser: ParameterParser[Seq[Int]] = new ParameterParser[Seq[Int]] {
    private val coIdsRegex = raw"(\d+)-(\d+)".r

    override def parse(value: String): Either[String, Seq[Int]] = {
      value match {
        case coIdsRegex(a, b) =>
          Try(a.toInt to b.toInt).toEither.left.map(_.getMessage)
        case other =>
          Try(other.split(",").toSeq.map(_.trim.toInt)).toEither.left.map(_.getMessage)
      }
    }

    override def example: Seq[Int] = Seq(1, 2, 3)
  }

  val paramSpec =
    for {
      _ <- metadata(programName = "Gis importer")
      outputFilePath <- namedParameter[Path]("Output json file", "file path", "o", "output-file")
      cadastralDistrict <- namedParameter[CadastralDistrictNumber](
        "Cadastral district",
        "district id",
        "cd",
        "cadastral-district"
      )
      cadastralObjectIds <- parameter[Seq[Int]]("Cadastral object ids", "ids")
    } yield CommandLineParameters(outputFilePath, cadastralDistrict, cadastralObjectIds)

  val get = parameters[CommandLineParameters]

  val layer = parametersFromArgs(paramSpec).printUsageInfoOnFailure
}
