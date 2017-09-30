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

import java.util.concurrent.{Executors, LinkedBlockingQueue, TimeUnit}

import akka.NotUsed
import akka.stream.scaladsl.Source
import akka.stream.stage.{GraphStage, GraphStageLogic, OutHandler, StageLogging}
import akka.stream.{Attributes, Outlet, SourceShape}
import com.netflix.astyanax.Keyspace
import com.netflix.astyanax.model.{ColumnFamily, Row}
import com.netflix.astyanax.recipes.reader.AllRowsReader

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

private[stream] class CassandraSource[K, C](keyspace: Keyspace,
                                            columnFamily: ColumnFamily[K, C],
                                            pageSize: Int,
                                            queueSize: Int,
                                            dequeueTimeout: Int)
    extends GraphStage[SourceShape[Row[K, C]]] {

  private[this] implicit val executionContext: ExecutionContext =
    ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor())

  val out: Outlet[Row[K, C]] = Outlet[Row[K, C]]("CassandraSource.out")

  override def shape: SourceShape[Row[K, C]] = SourceShape(out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) with StageLogging {

      // thread-safe concurrent FIFO
      val queue = new LinkedBlockingQueue[Row[K, C]](queueSize)

      def fullScanTable =
        new AllRowsReader.Builder[K, C](keyspace, columnFamily)
          .withPageSize(pageSize)
          .forEachRow(new com.google.common.base.Function[Row[K, C], java.lang.Boolean] {
            override def apply(input: Row[K, C]) = {
              // enqueue at the tail of the queue if there is space or wait
              queue.put(input)
              true
            }
          })
          .build()
          .call()

      override def preStart(): Unit = {
        super.preStart()
        log.debug("createLogic#preStart")

        // need to run in a different thread
        // otherwise put/poll from the queue will block each other
        Future(fullScanTable)
      }

      def pushInput: Unit =
        Try(queue.poll(dequeueTimeout, TimeUnit.SECONDS)) match {
          case Success(input) =>
            Option(input) match {
              // dequeue from the head of the queue
              case Some(_) =>
                push(out, input)
              // null if the specified waiting time elapses before an element is available
              case None =>
                log.warning("closing source: empty queue")
                complete(out)
            }
          case Failure(e: InterruptedException) =>
            log.error(e, s"restart polling: interrupted while waiting $e")
            // TODO exponential backoff ?
            pushInput
          case Failure(e) =>
            log.error(e, s"closing source: unhandled error $e")
            complete(out)
        }

      setHandler(out, new OutHandler {
        override def onPull(): Unit = pushInput
      })

      override def postStop(): Unit = {
        super.postStop()
        log.debug("createLogic#postStop")
      }

    }
}

object CassandraSource {
  protected[stream] val defaultPageSize = 1000
  protected[stream] val defaultQueueSize = 3000
  protected[stream] val defaultPollingTimeout = 5 // seconds

  def apply[K, C](keyspace: Keyspace,
                  columnFamily: ColumnFamily[K, C],
                  pageSize: Int = defaultPageSize,
                  queueSize: Int = defaultQueueSize,
                  dequeueTimeout: Int = defaultPollingTimeout): Source[Row[K, C], NotUsed] =
    Source.fromGraph(new CassandraSource(keyspace, columnFamily, pageSize, queueSize, dequeueTimeout))
}
