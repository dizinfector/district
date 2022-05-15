package xyz.district.gisimporter

import org.http4s.blaze.client.BlazeClientBuilder
import org.http4s.client.Client
import zio.{Executor, Task, TaskLayer}
import zio.interop.catz.*
import zio.interop.*

import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.{SSLContext, X509TrustManager}

/**
 * Http4s клиент
 */
object Http4sClient {
  private lazy val trustingSslContext: SSLContext = {
    val trustManager = new X509TrustManager {
      def getAcceptedIssuers: Array[X509Certificate] = Array.empty
      def checkClientTrusted(
        certs: Array[X509Certificate],
        authType: String
      ): Unit = {}
      def checkServerTrusted(
        certs: Array[X509Certificate],
        authType: String
      ): Unit = {}
    }
    val sslContext = SSLContext.getInstance("TLS")
    sslContext.init(Array(), Array(trustManager), new SecureRandom)
    sslContext
  }

  def layer(ex: Executor): TaskLayer[Client[Task]] = {

    BlazeClientBuilder[Task]
      .withExecutionContext(ex.asExecutionContext)
      .withSslContext(trustingSslContext)
      .resource
      .toManagedZIO
      .toLayer
  }
}
