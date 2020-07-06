package net.wiringbits.sdb

import ackcord.data.{GuildChannel, GuildId}

/**
 * There are some details specified on the config, but aren't complete,
 * this object is intended to hold the complete details required by the
 * whole app.
 */
class SharedState {
  import SharedState._

  private var servers: List[ServerDetails] = List.empty

  /**
   * The lock is not a problem because this is called only when the bot gets connected
   * to the discord server.
   */
  def add(server: ServerDetails): Unit = synchronized {
    servers = server :: servers
  }

  def findBy(guildId: GuildId): Option[ServerDetails] = servers.find(_.notificationChannel.id == guildId)

  def teamMembers(guildId: GuildId): List[TeamMember] = {
    servers
      .find(_.notificationChannel.id == guildId)
      .map(_.members)
      .getOrElse(List.empty)
  }
}

object SharedState {
  case class ServerDetails(notificationChannel: GuildChannel, members: List[TeamMember])
}
