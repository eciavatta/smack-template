package smack.cassandra

import com.google.common.util.concurrent.{FutureCallback, Futures, ListenableFuture}

import scala.concurrent.{Future, Promise}

object ScalaConverters {

  /* Taken from https://stackoverflow.com/a/19528638 */
  implicit class RichListenableFuture[T](lf: ListenableFuture[T]) {
    def asScala: Future[T] = {
      val p = Promise[T]()
      Futures.addCallback(lf, new FutureCallback[T] {
        def onFailure(t: Throwable): Unit = p failure t
        def onSuccess(result: T): Unit    = p success result
      })
      p.future
    }
  }

}
