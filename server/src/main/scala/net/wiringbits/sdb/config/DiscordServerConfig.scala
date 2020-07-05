package net.wiringbits.sdb.config

import com.typesafe.config.Config

import scala.collection.JavaConverters.asScalaBufferConverter

case class DiscordServerConfig(
    name: String,
    notificationsChannelName: String,
    members: List[String]
)

object DiscordServerConfig {

  def apply(config: Config): DiscordServerConfig = {
    val name = config.getString("name")
    val notificationsChannelName = config.getString("notificationsChannelName")
    val members = config
      .getStringList("members")
      .asScala
      .toList
      .map(_.trim)
      .filter(_.nonEmpty)
    DiscordServerConfig(name = name, notificationsChannelName = notificationsChannelName, members = members)
  }
}
