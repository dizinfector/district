package xyz.district.gisimporter

import io.circe.Decoder
import io.circe.generic.AutoDerivation
import nu.pattern.OpenCV
import org.opencv.core.{Mat, MatOfPoint, MatOfPoint2f, Point => OpencvPoint}
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import xyz.district.gisimporter.rosreestr.*
import xyz.district.gisimporter.rosreestr.{Feature, FeatureResponse}
import zio.stream.ZStream
import zio.nio.file.Path
import zio.{RIO, Task, TaskLayer, URIO, URLayer, ZIO, ZLayer}

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import scala.collection.mutable
import scala.jdk.CollectionConverters.*

case class Point(x: Double, y: Double)

/**
 * Служба для поиска контуров объекта на изображении
 */
trait ImageContourFinder {

  /**
   * Находит контуры объекта считая от левого верхнего угла изображения
   *
   * @param imagePath
   *   путь к файлу изображения
   * @return
   */
  def findContours(imagePath: Path): ZIO[Any, Throwable, Seq[Point]]
}

object ImageContourFinder {
  def findContours(imagePath: Path): RIO[ImageContourFinder, Seq[Point]] =
    ZIO.serviceWithZIO[ImageContourFinder](_.findContours(imagePath))
}

class OpenCVImageContourFinder extends ImageContourFinder {
  import OpenCVImageContourFinder.*

  /**
   * @inheritdoc
   */
  override def findContours(imagePath: Path): ZIO[Any, Throwable, Seq[Point]] = {
    for {
      mat <- ZIO.attempt(Imgcodecs.imread(imagePath.toString, Imgcodecs.IMREAD_GRAYSCALE))
      points <- ZIO.attempt {
        // Open CV не возвращает результатов, написан в C стиле c передачей пустых структур ответа
        val foundCoordinates = mutable.ListBuffer.empty[MatOfPoint].asJava

        Imgproc.findContours(mat, foundCoordinates, new Mat(), Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_TC89_KCOS)

        foundCoordinates.asScala.last.toList.asScala.toSeq
      }
      // пытаемся из нескольких десятков координат получить только координаты углов
      optimizedCoordinates <- ZIO.attempt {
        val resultBuffer = new MatOfPoint2f()

        Imgproc.approxPolyDP(new MatOfPoint2f(points: _*), resultBuffer, OPENCV_APPROX_EPSILON, false)

        resultBuffer.toList.asScala.toSeq.map(openCVPoint => Point(openCVPoint.x, openCVPoint.y))
      }
    } yield optimizedCoordinates
  }
}

object OpenCVImageContourFinder {

  /**
   * Константа для аппроксиматора OpenCV
   */
  val OPENCV_APPROX_EPSILON = 5

  val layer: TaskLayer[ImageContourFinder] =
    ZLayer {
      for {
        _ <- ZIO.attempt(OpenCV.loadLocally())
      } yield new OpenCVImageContourFinder
    }
}
