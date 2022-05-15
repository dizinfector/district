package xyz.district.gisimporter

import cats.effect.Async
import io.circe.Decoder
import org.http4s.{EntityDecoder, Headers, Request, Uri}
import org.http4s.blaze.client.BlazeClientBuilder
import org.http4s.circe.jsonOf
import org.http4s.client.Client
import org.http4s.headers.`Accept-Charset`
import org.http4s.Charset
import zio.interop.catz.*
import zio.interop.*
import zio.managed.ZManaged
import zio.{Executor, Tag, Task, TaskLayer, ULayer, URLayer, ZIO, ZLayer}
import zio.nio.file.Files
import zio.nio.file.Path
import fs2.io.file.Path as Fs2Path

import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.{SSLContext, X509TrustManager}
import scala.concurrent.ExecutionContextExecutor

/**
 * Фасад Http клиента
 */
trait HttpClientFacade {

  /**
   * Делает GET запрос и десериализует JSON в указанный тип
   *
   * @param uri url для запроса
   * @param parameters url параметры
   * @tparam T тип возврата
   * @return
   */
  def get[T: Decoder: Tag](uri: String, parameters: Map[String, String]): Task[T]

  /**
   * Делает GET запрос, сохраняет результат во временный файл
   *
   * @param uri url для запроса
   * @param parameters url параметры
   * @return
   */
  def getBinary(uri: String, parameters: Map[String, String]): Task[Path]
}

case class Http4sClientFacade(http4sClient: Client[Task]) extends HttpClientFacade {

  /**
   * @inheritdoc
   */
  override def get[T: Decoder: Tag](uri: String, parameters: Map[String, String]): Task[T] = {
    val resourceUri = Uri
      .fromString(uri)
      .getOrElse(throw new Exception("wrong uri"))
      .withQueryParams(parameters)

    http4sClient.expect(resourceUri)(jsonOf[Task, T])
  }

  /**
   * @inheritdoc
   */
  def getBinary(uri: String, parameters: Map[String, String]): Task[Path] = {
    val resourceUri = Uri
      .fromString(uri)
      .getOrElse(throw new Exception("wrong uri"))
      .withQueryParams(parameters)

    for {
      tmpFilePath <- Files.createTempFile(".png", None, Seq.empty)
      _ <- http4sClient.expect(resourceUri)(EntityDecoder.binFile(Fs2Path(tmpFilePath.toString)))
    } yield tmpFilePath
  }
}

object Http4sClientFacade {
  val layer: URLayer[Client[Task], HttpClientFacade] =
    ZLayer {
      for {
        http4sClient <- ZIO.service[Client[Task]]
      } yield Http4sClientFacade(http4sClient)
    }
}
