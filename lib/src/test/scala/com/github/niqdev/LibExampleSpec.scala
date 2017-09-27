package com.github.niqdev

import org.scalatest.{Matchers, WordSpecLike}

final class LibExampleSpec extends WordSpecLike with Matchers {

  "LibExample" must {

    "hello" in {
      LibExample.hello shouldBe "hello"
    }

  }

}
