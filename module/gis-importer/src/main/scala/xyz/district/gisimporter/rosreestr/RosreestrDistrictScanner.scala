package xyz.district.gisimporter.rosreestr

import fs2.io.file.Path as Fs2Path
import io.circe.Decoder
import io.circe.generic.AutoDerivation
import io.circe.generic.auto.*
import xyz.district.gisimporter.*
import xyz.district.gisimporter.model.DistrictCadastralObjects
import xyz.district.gisimporter.{CadastralDistrictNumber, CadastralNumber}
import xyz.district.gisimporter.rosreestr.*
import zio.logging.*
import zio.nio.file.{Files, Path}
import zio.stream.ZStream
import RosreestrDistrictScanner.expectedFeatureSpriteImageSize
import zio.{Cause, RIO, Task, URIO, URLayer, ZIO, ZLayer}

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Сканер на базе API Росреестра
 *
 * @param httpClient http клиент
 * @param imageContourFinder искатель контуров изображения
 * @param coordinatesTransformer трансформер координат
 */
case class RosreestrDistrictScanner(
  httpClient: HttpClientFacade,
  imageContourFinder: ImageContourFinder,
  coordinatesTransformer: CoordinatesTransformer
) extends DistrictScanner {

  /**
   * @inheritdoc
   */
  override def scan(
    districtNumber: CadastralDistrictNumber,
    cadastralObjectIds: Seq[Int],
    parallelism: Int
  )(implicit trace: zio.ZTraceElement): Task[DistrictCadastralObjects] = {
    ZIO
      .foreachPar(cadastralObjectIds) { id =>
        val cadastralNumber = CadastralNumber(districtNumber, id.toString)

        val scanEffect = for {
          _ <- ZIO.logInfo(s"Scanning $cadastralNumber")
          feature <- getFeature(cadastralNumber, FeatureType.values.toList)
          geometry <- getGeometry(feature)
        } yield Some((feature, geometry))

        scanEffect.catchAll { ex =>
          ZIO.logErrorCause(s"Unable to scan $cadastralNumber (${ex.getMessage})", Cause.fail(ex)) *>
            ZIO.succeed(Option.empty[(Feature, Geometry)])
        }
      }
      .withParallelism(parallelism)
      .map(_.collect { case Some(f) => f })
      .flatMap(transformScanResult)
  }

  /**
   * Пытается получить информацию о кадастровом объекте рекурсивно перебирая типы
   *
   * @param cadastralNumber кадастровый номер
   * @param types оставшиеся типы для перебора
   * @param trace трассировщик ZIO
   * @return
   */
  private def getFeature(cadastralNumber: CadastralNumber, types: Seq[FeatureType])(implicit
    trace: zio.ZTraceElement
  ): Task[Feature] = {
    types match {
      case Nil =>
        ZIO.fail(new Exception(s"Unable to find any feature from list ${types.mkString(", ")} of $cadastralNumber"))
      case firstType :: rest =>
        httpClient
          .get[FeatureResponse](RosreestrUrl.getFeature(cadastralNumber, firstType), Map.empty)
          .map(_.feature)
          .orElse(getFeature(cadastralNumber, rest))
    }
  }

  /**
   * Получаем геометрию объекта
   * Сначала получаем "спрайт" объекта для карты в виде файла,
   * затем по изображению ищем контуры (координаты пикселей изображения),
   * контуры переводим в координаты EPSG 3785
   *
   * @param feature кадастровый объект
   * @param trace трассировщик ZIO
   * @return
   */
  private def getGeometry(feature: Feature)(implicit trace: zio.ZTraceElement): Task[Geometry] = {
    for {
      _ <- ZIO.logDebug(s"Getting ${feature.attrs.cn} geometry")
      spriteSize = expectedFeatureSpriteImageSize(feature)
      (url, params) = RosreestrUrl.exportMapSprite(feature, spriteSize)
      filePath <- httpClient.getBinary(url, params)
      coordinates <- ZIO.scoped {
        imageContourFinder
          .findContours(filePath)
          .flatMap(pointsToCoordinates(_, spriteSize, feature.extent))
          .withFinalizer(_ => Files.delete(filePath).ignoreLogged)
      }
    } yield Geometry(coordinates)
  }

  /**
   * Переводит контуры изображения в координаты объекта EPSG 3785
   *
   * @param points контуры изображения
   * @param imageSize размер изображения
   * @param extent объект границ области
   * @param trace трассировщик ZIO
   * @return
   */
  private def pointsToCoordinates(points: Seq[Point], imageSize: SpriteImageSize, extent: Extent)(implicit
    trace: zio.ZTraceElement
  ): Task[Seq[Coordinate]] = {
    ZIO.attempt {
      val boundXDelta = (extent.xmax - extent.xmin) * 0.01
      val boundYDelta = (extent.ymax - extent.ymin) * 0.01

      points.map(point =>
        // точки в изображении начинаются от верхнего левого угла, а координаты от нижнего левого, потому переводим
        val y = imageSize.height - point.y

        Coordinate(
          extent.xmin + boundXDelta * (point.x / imageSize.width) * 100,
          extent.ymin + boundYDelta * (y / imageSize.height) * 100
        )
      )
    }
  }

  /**
   * Переводит кадастровые объекты в формат вывода
   *
   * @param featuresWithGeometry кадастровые объекты с геометрией
   * @return
   */
  def transformScanResult(featuresWithGeometry: Seq[(Feature, Geometry)]): Task[DistrictCadastralObjects] = {
    ZIO
      .collectAll(featuresWithGeometry.map { case (feature, geometry) =>
        for {
          coordinates <- ZIO.collectAll(geometry.coordinates.map(coordinatesTransformer.epsg3785toGps))
          cadastralNumber <- ZIO.fromTry(CadastralNumber.fromString(feature.attrs.cn))
        } yield (feature.enumType, feature.attrs.address, coordinates, cadastralNumber)
      })
      .map { transformData =>
        val groupedByType = transformData.groupBy { case (featureType, _, _, _) =>
          featureType
        }

        DistrictCadastralObjects(
          groupedByType
            .getOrElse(FeatureType.Building, Seq.empty)
            .map { (_, address, coordinates, cadastralNumber) =>
              model.Building(address, coordinates, cadastralNumber)
            },
          groupedByType
            .getOrElse(FeatureType.LandPlot, Seq.empty)
            .map { (_, address, coordinates, cadastralNumber) =>
              model.LandPlot(address, coordinates, cadastralNumber)
            }
        )
      }
  }
}

object RosreestrDistrictScanner {
  // необходимая ширина изображения
  private val ImageSpriteHeight = 200

  /**
   * Рассчитать размер изображения исходя из пропорций границ области
   *
   * @param feature данные кадастрового объекта
   * @return
   */
  def expectedFeatureSpriteImageSize(feature: Feature): SpriteImageSize = {
    val aspect = (feature.extent.xmax - feature.extent.xmin) / (feature.extent.ymax - feature.extent.ymin)

    SpriteImageSize((ImageSpriteHeight * aspect).toInt, ImageSpriteHeight)
  }

  val layer: URLayer[HttpClientFacade & ImageContourFinder & CoordinatesTransformer, DistrictScanner] =
    ZLayer {
      for {
        httpClient <- ZIO.service[HttpClientFacade]
        imageContourFinder <- ZIO.service[ImageContourFinder]
        coordinatesTransformer <- ZIO.service[CoordinatesTransformer]
      } yield RosreestrDistrictScanner(httpClient, imageContourFinder, coordinatesTransformer)
    }
}
