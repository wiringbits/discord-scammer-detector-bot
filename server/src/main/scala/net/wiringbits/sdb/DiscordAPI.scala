package net.wiringbits.sdb

import ackcord.data.raw.RawGuildMember
import ackcord.data.{GuildChannel, GuildId, TextChannelId, UserId}
import ackcord.requests.{
  CreateGuildBanData,
  CreateMessage,
  CreateMessageData,
  GetCurrentUserGuildsData,
  GetUserGuildsGuild
}
import ackcord.{CacheSnapshot, DiscordClient}
import net.wiringbits.sdb.config.{DiscordServerConfig, WhitelistedServersConfig}
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

/**
 * A high-level API to interact with discord based on our config
 */
class DiscordAPI(config: WhitelistedServersConfig, client: DiscordClient)(implicit ec: ExecutionContext) {

  private val logger = LoggerFactory.getLogger(this.getClass)

  /**
   * Get the guilds where the bot is installed, and where the app is configured to use.
   */
  def getSupportedGuilds(implicit c: CacheSnapshot): Future[Seq[(DiscordServerConfig, GetUserGuildsGuild)]] = {
    getGuilds()
      .map { list =>
        list.flatMap { guild =>
          config.servers.find(_.name == guild.name).map(_ -> guild)
        }
      }
  }

  /**
   * Gets the guild channel that the bot uses to send notifications.
   */
  def getNotificationChannel(guildId: GuildId, config: DiscordServerConfig)(
      implicit c: CacheSnapshot
  ): Future[Option[GuildChannel]] = {
    getChannels(guildId)
      .map { channels =>
        channels.find { channel =>
          channel.name.contains(config.notificationsChannelName)
        }
      }
  }

  /**
   * Get the info for a given member.
   */
  def getMember(guildId: GuildId, userId: UserId)(implicit c: CacheSnapshot): Future[Option[RawGuildMember]] = {
    val request = ackcord.requests.GetGuildMember(guildId, userId)
    client.requestsHelper
      .run(request)
      .value
  }

  def banMember(guildId: GuildId, userId: UserId)(implicit c: CacheSnapshot): Future[Unit] = {
    val params = CreateGuildBanData(None, reason = Some("Scammer detected"))
    val request = ackcord.requests.CreateGuildBan(guildId, userId, queryParams = params, reason = params.reason)
    client.requestsHelper
      .run(request)
      .value
      .map(_ => ())
  }

  /**
   * Send a message to the given channel, in case of failure, just log a warning.
   */
  def sendMessage(channel: GuildChannel, msg: String)(implicit c: CacheSnapshot): Unit = {
    val textChannelId = TextChannelId(channel.id.toString)
    val request = CreateMessage(textChannelId, CreateMessageData(content = msg))
    client.requestsHelper.run(request).value.onComplete {
      case Success(_) => ()
      case Failure(ex) =>
        logger.warn(s"Failed to send message, guild = ${channel.guildId}, channel = ${channel.name}, msg = $msg", ex)
    }
  }

  private def getGuilds()(implicit c: CacheSnapshot): Future[Seq[GetUserGuildsGuild]] = {
    val request = ackcord.requests.GetCurrentUserGuilds(GetCurrentUserGuildsData())
    client.requestsHelper
      .run(request)
      .value
      .map(_.getOrElse(List.empty))
  }

  private def getChannels(guildId: GuildId)(implicit c: CacheSnapshot): Future[Seq[GuildChannel]] = {
    val request = ackcord.requests.GetGuildChannels(guildId)
    client.requestsHelper
      .run(request)
      .value
      .map(_.getOrElse(List.empty))
      .map(_.flatMap(_.toGuildChannel(guildId)))
  }
}
