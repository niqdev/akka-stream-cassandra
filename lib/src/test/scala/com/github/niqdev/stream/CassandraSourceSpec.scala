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
import com.netflix.astyanax.model.ColumnFamily
import com.netflix.astyanax.serializers.StringSerializer

import scala.collection.JavaConverters.mapAsJavaMapConverter
import scala.concurrent.ExecutionContext

final class CassandraSourceSpec
    extends TestKit(ActorSystem("test-actor-system"))
    with BaseSpec
    with ImplicitSender
    with StopSystemAfterAll
    with EmbeddedCassandraSupport {

  private[this] implicit val materializer: ActorMaterializer = ActorMaterializer()
  private[this] implicit val executionContext: ExecutionContext = system.dispatcher

  private[this] val defaultTtl = 9999
  private[this] val columnFamily: ColumnFamily[String, String] =
    new ColumnFamily[String, String]("example", StringSerializer.get, StringSerializer.get)

  override protected def beforeAll: Unit = {
    super.beforeAll
    startEmbeddedCassandra
    initCassandra
  }

  private[this] def insertRow(rowKey: String, column: String, value: String, ttl: Int = defaultTtl) =
    getKeyspace
      .prepareColumnMutation(columnFamily, rowKey, column)
      .putValue(value, ttl)
      .execute()

  private[this] def initCassandra = {
    getKeyspace.createColumnFamily(columnFamily, Map.empty[String, AnyRef].asJava)
    columnFamily.describe(getKeyspace)

    insertRow("rowKey1", "column1", "value1")
    insertRow("rowKey1", "column2", "value2")
    insertRow("rowKey1", "column3", "value3")

    insertRow("rowKey2", "column1", "value1")
    insertRow("rowKey2", "column2", "value2")

    insertRow("rowKey3", "column1", "value1")
    insertRow("rowKey3", "column2", "value2")
  }

  override protected def afterAll(): Unit = {
    super.afterAll()
    getKeyspace.dropColumnFamily(columnFamily)
    shutdownEmbeddedCassandra
  }

  "CassandraSource" must {

    "verify row keys" in {
      val (_, subscriber) = CassandraSource(getKeyspace, columnFamily)
        .map(_.getKey)
        .toMat(TestSink.probe[String])(Keep.both)
        .run()

      subscriber.request(3)

      subscriber.expectNextUnorderedN(Vector("rowKey1", "rowKey2", "rowKey3"))
    }

  }

}
