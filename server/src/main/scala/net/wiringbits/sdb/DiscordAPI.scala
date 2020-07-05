package net.wiringbits.sdb

import ackcord.data.{GuildChannel, GuildId, TextChannelId}
import ackcord.requests.{CreateMessage, CreateMessageData, GetCurrentUserGuildsData, GetUserGuildsGuild}
import ackcord.{CacheSnapshot, DiscordClient}
import net.wiringbits.sdb.config.{DiscordServerConfig, WhitelistedServersConfig}
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class DiscordAPI(config: WhitelistedServersConfig, client: DiscordClient)(implicit ec: ExecutionContext) {

  private val logger = LoggerFactory.getLogger(this.getClass)

  def getSupportedGuilds(implicit c: CacheSnapshot): Future[Seq[(DiscordServerConfig, GetUserGuildsGuild)]] = {
    getGuilds()
      .map { list =>
        list.flatMap { guild =>
          config.servers.find(_.name == guild.name).map(_ -> guild)
        }
      }
  }

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

  def getGuilds()(implicit c: CacheSnapshot): Future[Seq[GetUserGuildsGuild]] = {
    val request = ackcord.requests.GetCurrentUserGuilds(GetCurrentUserGuildsData())
    client.requestsHelper
      .run(request)
      .value
      .map(_.getOrElse(List.empty))
  }

  def getChannels(guildId: GuildId)(implicit c: CacheSnapshot): Future[Seq[GuildChannel]] = {
    val request = ackcord.requests.GetGuildChannels(guildId)
    client.requestsHelper
      .run(request)
      .value
      .map(_.getOrElse(List.empty))
      .map(_.flatMap(_.toGuildChannel(guildId)))
  }

  def sendMessage(channel: GuildChannel, msg: String)(implicit c: CacheSnapshot): Unit = {
    val textChannelId = TextChannelId(channel.id.toString)
    val request = CreateMessage(textChannelId, CreateMessageData(content = msg))
    client.requestsHelper.run(request).value.onComplete {
      case Success(_) => ()
      case Failure(ex) =>
        logger.warn(s"Failed to send message, guild = ${channel.guildId}, channel = ${channel.name}, msg = $msg", ex)
    }
  }
}
