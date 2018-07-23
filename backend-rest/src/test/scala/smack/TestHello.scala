package smack

import org.scalatest.MustMatchers
import org.scalatest.WordSpecLike

class TestHello extends WordSpecLike with MustMatchers {

  "Hello service" must {
    "Reply with the correct message" in {
      val hello = new Hello()
      hello.sayHello("world") mustEqual "Hello, world"
    }
  }

}
