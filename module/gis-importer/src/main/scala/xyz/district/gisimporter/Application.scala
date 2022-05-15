package xyz.district.gisimporter

import io.github.vigoo.clipp.ParserFailure
import xyz.district.gisimporter.model.*
import zio.Console.{printLine, readLine}
import zio.*
import xyz.district.gisimporter.DistrictScanner
import xyz.district.gisimporter.rosreestr.{RosreestrDistrictScanner, Feature, FeatureType}
import zio.config.syntax.*
import zio.config.typesafe.*
import zio.config.*
import zio.config.magnolia.descriptor
import zio.logging.LogFormat.{label, line, quoted}
import zio.logging.backend.SLF4J
import zio.nio.channels.AsynchronousFileChannel
import zio.nio.file.Path

import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.StandardOpenOption

trait ApplicationBase {

  /**
   * Основной эффект приложения
   *
   * @param params параметры из коммандной строки
   * @param config конфигурация
   * @return
   */
  def program(params: CommandLineParameters, config: Config) = for {
    _ <- ZIO.logInfo(s"Running the program with params $params $config")
    cadastralObjects <- DistrictScanner.scan(
      params.cadastralDistrictNumber,
      params.cadastralObjectIds,
      config.parallelism
    )
    _ <- CadastralDataWriter.writeDistrictCadastralObjects(params.outputFilePath, cadastralObjects)
  } yield ()
}

object Application extends ApplicationBase with ZIOAppDefault {

  /**
   * Хук подменяет стандартный логгер на slf4j
   *
   * @return
   */
  override def hook =
    RuntimeConfigAspect(self => self.copy(logger = ZLogger.none)) >>> SLF4J.slf4j(LogLevel.All, line)

  /**
   * Входная точка приложения
   * - парсит аргументы
   * - читает конфиг
   * - запускает основной эффект с нужными зависимостями
   *
   * @return
   */
  override def run = for {
    ex <- ZIO.executor
    args <- CommandLineParameters.get.provideLayer(CommandLineParameters.layer)
    config <- config.read(descriptor[Config] from TypesafeConfigSource.fromResourcePath)
    _ <- program(args, config)
      .provide(
        Http4sClient.layer(ex),
        Http4sClientFacade.layer,
        RosreestrDistrictScanner.layer,
        CoordinatesTransformerImpl.layer,
        OpenCVImageContourFinder.layer,
        CadastralDataJsonWriter.layer
      )
  } yield ExitCode.success
}
