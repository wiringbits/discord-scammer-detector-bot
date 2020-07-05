package net.wiringbits.sdb.config

import com.typesafe.config.Config

case class DiscordConfig(token: String, whitelistedServersConfig: WhitelistedServersConfig)

object DiscordConfig {

  def apply(config: Config): DiscordConfig = {
    val whitelistedServers = WhitelistedServersConfig(config)
    val token = config.getString("token")
    DiscordConfig(token, whitelistedServers)
  }
}
