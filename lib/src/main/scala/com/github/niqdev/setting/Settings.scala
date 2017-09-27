package com.github.niqdev
package setting

import com.typesafe.config.ConfigFactory

object Settings {
  private[this] lazy val config = ConfigFactory.load()

  object Application {
    private[this] lazy val applicationConfig = config getConfig "application"

    val name: String = applicationConfig getString "name"
  }

}
