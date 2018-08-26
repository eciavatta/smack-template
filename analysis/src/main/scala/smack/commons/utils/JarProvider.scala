package smack.commons.utils

import java.io.{File, FileInputStream}

import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoHTTPD.Response.Status
import fi.iki.elonen.NanoHTTPD.{IHTTPSession, Response}

class JarProvider(jarFile: File, port: Int) extends NanoHTTPD(port) {

  start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)

  override def serve(session: IHTTPSession): Response = {
    val inputStream = new FileInputStream(jarFile)
    NanoHTTPD.newChunkedResponse(Status.OK, "application/java-archive", inputStream)
  }

}
