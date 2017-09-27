package com.github.niqdev

import com.github.niqdev.setting.Settings
import com.typesafe.scalalogging.Logger

object LibExample {
  private[this] val log = Logger(getClass.getName)

  def hello: String = "hello"

  def print(value: String): Unit = log.debug(s"${Settings.Application.name}: $value")
}
