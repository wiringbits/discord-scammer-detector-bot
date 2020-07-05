package net.wiringbits.sdb

import ackcord.data.{GuildChannel, GuildId}

class SharedState {
  import SharedState._

  private var channels: List[Channel] = List.empty

  def addChannel(channel: Channel): Unit = synchronized {
    channels = channel :: channels
  }

  def findBy(guildId: GuildId): Option[Channel] = channels.find(_.guildChannel.id == guildId)

  def teamMembers(guildId: GuildId): List[TeamMember] = {
    channels
      .find(_.guildChannel.id == guildId)
      .map(_.members)
      .getOrElse(List.empty)
  }
}

object SharedState {
  case class Channel(guildChannel: GuildChannel, members: List[TeamMember])
}
