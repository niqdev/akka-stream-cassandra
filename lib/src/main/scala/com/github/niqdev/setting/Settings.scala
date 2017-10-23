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
package setting

import com.typesafe.config.ConfigFactory

object Settings {
  private[this] lazy val config = ConfigFactory.load()

  object Application {
    private[this] lazy val applicationConfig = config getConfig "application"

    val name: String = applicationConfig getString "name"
  }

  object Library {
    private[this] lazy val libConfig = config getConfig "akka-stream-cassandra"

    val parallelism: Int = libConfig getInt "parallelism"
    val pageSize: Int = libConfig getInt "page-size"
    val queueSize: Int = libConfig getInt "queue-size"
    val dequeueTimeout: Int = libConfig getInt "dequeue-timeout"
  }

}
