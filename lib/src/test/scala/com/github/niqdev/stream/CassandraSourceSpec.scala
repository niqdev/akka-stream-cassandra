/*
 * Copyright (c) 2017 niqdev
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.github.niqdev
package stream

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Keep
import akka.stream.testkit.scaladsl.TestSink
import akka.testkit.{ImplicitSender, TestKit}

import scala.concurrent.ExecutionContext

final class CassandraSourceSpec
    extends TestKit(ActorSystem("test-actor-system"))
    with BaseSpec
    with ImplicitSender
    with StopSystemAfterAll {

  private[this] implicit val materializer: ActorMaterializer = ActorMaterializer()
  private[this] implicit val executionContext: ExecutionContext = system.dispatcher

  "CassandraSource" must {

    "verify counter" in {
      val (_, subscriber) = CassandraSource()
        .take(10)
        .fold(0)(_ + _)
        .toMat(TestSink.probe[Int])(Keep.both)
        .run()

      subscriber.request(1)
      subscriber.expectNextPF {
        case result => result shouldBe 55
      }
    }

  }

}
