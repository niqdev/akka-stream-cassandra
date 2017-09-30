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

import com.google.common.collect.ImmutableMap
import com.netflix.astyanax.connectionpool.NodeDiscoveryType
import com.netflix.astyanax.connectionpool.impl.{
  ConnectionPoolConfigurationImpl,
  ConnectionPoolType,
  CountingConnectionPoolMonitor
}
import com.netflix.astyanax.cql.test.utils.AstyanaxContextFactory
import com.netflix.astyanax.impl.AstyanaxConfigurationImpl
import com.netflix.astyanax.thrift.ThriftFamilyFactory
import com.netflix.astyanax.util.SingletonEmbeddedCassandra
import com.netflix.astyanax.{AstyanaxContext, Keyspace}

/*
 * https://github.com/Netflix/astyanax/search?q=embedded
 * https://github.com/Netflix/astyanax/tree/0146a6fc6ad61c8eb0173e1f4dca9429a3f1e969
 */
trait EmbeddedCassandraSupport { this: BaseSpec =>

  private[this] val sleepMillis = 3000
  private[this] var keyspace: Keyspace = _

  def getKeyspace: Keyspace = keyspace

  def startEmbeddedCassandra: Unit = {
    log.debug("start embedded cassandra")
    SingletonEmbeddedCassandra.getInstance()
    Thread.sleep(sleepMillis)
    createKeyspace
  }

  def createKeyspace = {
    val keyspaceContext = new AstyanaxContext.Builder()
      .forCluster("ClusterName")
      .forKeyspace("KeyspaceName")
      .withAstyanaxConfiguration(new AstyanaxConfigurationImpl()
        .setDiscoveryType(NodeDiscoveryType.RING_DESCRIBE)
        .setConnectionPoolType(ConnectionPoolType.TOKEN_AWARE))
      .withConnectionPoolConfiguration(
        new ConnectionPoolConfigurationImpl("MyConnectionPool")
          .setSocketTimeout(30000)
          .setMaxTimeoutWhenExhausted(2000)
          .setMaxConnsPerHost(20)
          .setInitConnsPerHost(10)
          .setSeeds("127.0.0.1:9160"))
      .withConnectionPoolMonitor(new CountingConnectionPoolMonitor())
      .buildKeyspace(ThriftFamilyFactory.getInstance())

    keyspaceContext.start()
    keyspace = keyspaceContext.getClient

    val options = ImmutableMap
      .builder[String, AnyRef]()
      .put("strategy_options",
           ImmutableMap
             .builder[String, AnyRef]()
             .put("replication_factor", "1")
             .build())
      .put("strategy_class", "SimpleStrategy")
      .build()
    keyspace.createKeyspace(options)
  }

  def shutdownEmbeddedCassandra: Unit = {
    log.debug("shutdown embedded cassandra")
    keyspace.dropKeyspace
    SingletonEmbeddedCassandra.getInstance().shutdown()
  }

}
