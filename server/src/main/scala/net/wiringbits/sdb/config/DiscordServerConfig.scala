package net.wiringbits.sdb.config

import com.typesafe.config.Config

import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.util.Try

case class DiscordServerConfig(
    name: String,
    notificationsChannelName: String,
    members: List[String],
    blacklist: Option[List[String]]
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
    val blacklist = Try(
      Option(
        config
          .getStringList("blacklist")
          .asScala
          .toList
          .map(_.trim)
      )
    ).getOrElse(None)

    DiscordServerConfig(
      name = name,
      notificationsChannelName = notificationsChannelName,
      members = members,
      blacklist = blacklist
    )
  }
}
