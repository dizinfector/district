package xyz.district.gisimporter

import io.circe.generic.AutoDerivation
import io.circe.generic.semiauto.*
import io.circe.*
import io.circe.syntax.*
import nu.pattern.OpenCV
import org.opencv.core.{Mat, MatOfPoint, MatOfPoint2f, Point as OpencvPoint}
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import xyz.district.gisimporter.model.{Building, DistrictCadastralObjects, GpsCoordinate, LandPlot}
import zio.nio.channels.AsynchronousFileChannel
import zio.nio.file.Path
import zio.stream.ZStream
import zio.{Chunk, RIO, Task, ULayer, URIO, ZIO, ZLayer}

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.nio.file.StandardOpenOption
import scala.collection.mutable
import scala.jdk.CollectionConverters.*

trait CadastralDataWriter {

  /**
   * Записывает кадастровые объекты в файл
   *
   * @param outputFilePath путь к файлу для записи
   * @param data данные для записи
   * @return
   */
  def writeDistrictCadastralObjects(outputFilePath: Path, data: DistrictCadastralObjects): Task[Unit]
}

object CadastralDataWriter {

  /**
   * Записывает кадастровые объекты в файл
   *
   * @param outputFilePath путь к файлу для записи
   * @param data данные для записи
   * @return
   */
  def writeDistrictCadastralObjects(
    outputFilePath: Path,
    data: DistrictCadastralObjects
  ): RIO[CadastralDataWriter, Unit] =
    ZIO.serviceWithZIO[CadastralDataWriter](_.writeDistrictCadastralObjects(outputFilePath, data))
}

class CadastralDataJsonWriter extends CadastralDataWriter {

  /**
   * @inheritdoc
   */
  def writeDistrictCadastralObjects(outputFilePath: Path, data: DistrictCadastralObjects): Task[Unit] = {
    import CadastralDataJsonWriter.dcoEncoder

    for {
      str <- ZIO.attempt(data.asJson.spaces2)
      _ <- ZIO.scoped(
        AsynchronousFileChannel
          .open(
            outputFilePath,
            StandardOpenOption.CREATE,
            StandardOpenOption.WRITE,
            StandardOpenOption.TRUNCATE_EXISTING
          )
          .flatMap(ch => ch.writeChunk(Chunk.fromIterable(str.getBytes(StandardCharsets.UTF_8)), 0))
      )
    } yield ()
  }
}

object CadastralDataJsonWriter {
  given cnEncoder: Encoder[CadastralNumber] = (cadastralNumber: CadastralNumber) =>
    Json.fromString(cadastralNumber.formatted)
  given gcEncoder: Encoder[GpsCoordinate] = (coordinate: GpsCoordinate) =>
    Json.fromValues(Seq(Json.fromDoubleOrNull(coordinate.lat), Json.fromDoubleOrNull(coordinate.lon)))
  given bEncoder: Encoder[Building] = deriveEncoder[Building]
  given lpEncoder: Encoder[LandPlot] = deriveEncoder[LandPlot]
  given dcoEncoder: Encoder[DistrictCadastralObjects] = deriveEncoder[DistrictCadastralObjects]

  val layer: ULayer[CadastralDataWriter] = ZLayer.succeed(new CadastralDataJsonWriter)
}
