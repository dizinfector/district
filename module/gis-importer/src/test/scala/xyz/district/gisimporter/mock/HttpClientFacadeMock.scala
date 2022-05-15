package xyz.district.gisimporter.mock

import io.circe.Decoder
import xyz.district.gisimporter.HttpClientFacade
import zio.internal.stacktracer.Tracer
import zio.mock.{Mock, Proxy}
import zio.nio.file.Path
import zio.{EnvironmentTag, Tag, Task, URLayer, ZIO, ZLayer}

object HttpClientFacadeMock extends Mock[HttpClientFacade] {

  object Get extends Poly.Effect.Output[(String, Map[String, String]), Throwable]
  object GetBinary extends Effect[(String, Map[String, String]), Throwable, Path]

  val compose: URLayer[Proxy, HttpClientFacade] = {
    given trace: Tracer.instance.Type = Tracer.newTrace

    ZLayer.fromZIO(
      ZIO
        .service[Proxy]
        .map(proxy =>
          new HttpClientFacade {
            override def get[T: Decoder: Tag](uri: String, parameters: Map[String, String]): Task[T] =
              proxy(Get.of[T], uri, parameters)

            override def getBinary(uri: String, parameters: Map[String, String]): Task[Path] =
              proxy(GetBinary, uri, parameters)
          }
        )
    )
  }
}
