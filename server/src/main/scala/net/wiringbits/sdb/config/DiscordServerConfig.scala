package net.wiringbits.sdb.config

import com.typesafe.config.Config

import scala.jdk.CollectionConverters.CollectionHasAsScala

case class DiscordServerConfig(
    name: String,
    notificationsChannelName: String,
    members: List[String],
    forbiddenWords: List[String]
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
    val forbiddenWords =
      config
        .getStringList("forbiddenWords")
        .asScala
        .toList
        .map(_.trim)

    DiscordServerConfig(
      name = name,
      notificationsChannelName = notificationsChannelName,
      members = members,
      forbiddenWords = forbiddenWords
    )
  }
}
