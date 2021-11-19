package net.wiringbits.sdb

import ackcord.data.{GuildChannel, GuildId}

/**
 * There are some details specified on the config, but aren't complete,
 * this object is intended to hold the complete details required by the
 * whole app.
 */
class SharedState {
  import SharedState._

  private var servers: Map[GuildId, ServerDetails] = Map.empty

  /**
   * The lock is not a problem because this is called only when the bot gets connected
   * to the discord server.
   */
  def add(server: ServerDetails): Unit = synchronized {
    servers = servers + (server.notificationChannel.guildId -> server)
  }

  def findBy(guildId: GuildId): Option[ServerDetails] = {
    servers.get(guildId)
  }

  def teamMembers(guildId: GuildId): List[TeamMember] = {
    servers
      .get(guildId)
      .map(_.members)
      .getOrElse(List.empty)
  }
}

object SharedState {
  case class ServerDetails(
      notificationChannel: GuildChannel,
      members: List[TeamMember],
      blacklist: Option[List[String]]
  )
}
