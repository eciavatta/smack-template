package smack.commons.traits

import akka.testkit.TestKitBase
import org.scalatest.{BeforeAndAfterAll, Suite}

trait AfterAllShutdown extends BeforeAndAfterAll {
  this: TestKitBase with Suite =>

  override def afterAll(): Unit = {
    super.afterAll()
    shutdown()
  }

}
