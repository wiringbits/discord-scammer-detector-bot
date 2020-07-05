package net.wiringbits.sdb

import ackcord.data.{GuildChannel, GuildId}
import net.wiringbits.sdb.config.WhitelistedServersConfig

class SharedState(config: WhitelistedServersConfig) {
  private var channels: List[GuildChannel] = List.empty

  def addChannel(channel: GuildChannel): Unit = synchronized {
    channels = channel :: channels
  }

  def findBy(guildId: GuildId): Option[GuildChannel] = channels.find(_.guildId == guildId)

  def whitelistedMembers(guildId: GuildId): List[String] = {
    val nameMaybe = channels.find(_.guildId == guildId).map(_.name)

    config.servers
      .find(nameMaybe.contains)
      .map(_.members)
      .getOrElse(List.empty)
  }
}
