package net.wiringbits.sdb.config

import com.typesafe.config.Config

import scala.jdk.CollectionConverters.CollectionHasAsScala

case class WhitelistedServersConfig(servers: List[DiscordServerConfig])

object WhitelistedServersConfig {

  def apply(config: Config): WhitelistedServersConfig = {
    val list = config.getConfigList("servers").asScala.toList.map(DiscordServerConfig.apply)
    WhitelistedServersConfig(list)
  }
}
