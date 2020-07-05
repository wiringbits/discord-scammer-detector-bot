package net.wiringbits.sdb

import ackcord._
import ackcord.data._
import ackcord.requests._
import cats.data._
import cats.implicits._
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.sslconfig.util.ConfigLoader
import org.apache.commons.text.similarity.LevenshteinDistance
import org.slf4j.LoggerFactory

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success}

case class ServerConfig(
    name: String,
    notificationsChannelName: String
)

object Main extends {

  private val logger = LoggerFactory.getLogger(this.getClass)
  private val WhitelistedServers = List(
//    ServerConfig("AlexITC's test server", "general"),
    ServerConfig("Stakenet (XSN)", "bots")
  )

  private val DistanceThreeshold = 2
  private val distance = new LevenshteinDistance(DistanceThreeshold)

  def main(args: Array[String]): Unit = {
    val config = ConfigFactory.load()
    val token = config.getString("discord.token")

    val clientSettings = ClientSettings(token)
    import clientSettings.executionContext

    // In real code, please dont block on the client construction
    val client = Await.result(clientSettings.createClient(), Duration.Inf)

    client.onEventSideEffects { implicit c: CacheSnapshot =>
      {
        case APIMessage.Ready(_) =>
          onReady(client)

        case e: APIMessage.GuildCreate =>
          logger.info(s"Bot installed on ${e.guild.name}")

        case e: APIMessage.GuildMemberAdd =>
          logger.info(s"${e.guild.name} - Member add: nickname = ${e.member.nick}, ${e.member.user.map(_.username)}")
          for {
            channel <- channels.find(_.guildId == e.guild.id)
            user <- e.member.user
          } yield handleUser(client, channel, user, e.member.nick)

        case e: APIMessage.GuildMemberUpdate =>
          logger.info(s"${e.guild.name} - Member update: newNickname = ${e.nick}, username = ${e.user.username}")
          channels
            .find(_.guildId == e.guild.id)
            .foreach { channel =>
              handleUser(client, channel, e.user, e.nick)
            }

//        case e: APIMessage.UserUpdate =>
//          println(s"User updated: ${e.user.username}")
////          channels
////            .find(_.guildId == e.user)
////            .foreach { channel =>
////              handleUser(client, channel, e.user)
////            }
//          OptionT(Future.successful(Some(())))
        case _ =>
      }
    }

    client.login()
  }

  private def getGuilds(
      client: DiscordClient
  )(implicit c: CacheSnapshot, ec: ExecutionContext): Future[Seq[GetUserGuildsGuild]] = {
    val request = ackcord.requests.GetCurrentUserGuilds(GetCurrentUserGuildsData())
    client.requestsHelper
      .run(request)
      .value
      .map(_.getOrElse(List.empty))
  }

  private var channels: List[GuildChannel] = List.empty
  private def addChannel(channel: GuildChannel): Unit = synchronized {
    channels = channel :: channels
  }

  private def onReady(client: DiscordClient)(implicit c: CacheSnapshot, ec: ExecutionContext): Unit = {
    logger.info("Ready")
    getGuilds(client)
      .map { list =>
        list.flatMap { guild =>
          WhitelistedServers.find(_.name == guild.name).map(_ -> guild)
        }
      }
      .foreach { guilds =>
        guilds.map {
          case (config, guild) =>
            val request = ackcord.requests.GetGuildChannels(guild.id)
            client.requestsHelper
              .run(request)
              .value
              .map(_.getOrElse(List.empty))
              .map { channels =>
                channels
                  .filter { channel =>
                    channel.`type` == ChannelType.GuildText &&
                    channel.name.contains(config.notificationsChannelName)
                  }
                  .flatMap { channel =>
                    channel.toGuildChannel(guild.id)
                  }
                  .headOption
                  .foreach { channel =>
                    logger.info(s"Got guild: name = ${guild.name}, notifications channel = ${channel.name}")
                    addChannel(channel)
                  }
              }
        }
      }
  }

  private def handlePotentialScammer(
      client: DiscordClient,
      channel: GuildChannel,
      scammerMention: String,
      relatedTeamMember: String
  )(
      implicit c: CacheSnapshot,
      ec: ExecutionContext
  ): Unit = {
    val msg =
      s"Potential scammer: $scammerMention looks very similar to our team member $relatedTeamMember"
    sendMessage(client, channel, msg)
  }

  private def handleUser(client: DiscordClient, channel: GuildChannel, user: User, nick: Option[String])(
      implicit c: CacheSnapshot,
      ec: ExecutionContext
  ): Unit = {
    val relatedMemberMaybe = findSimilarTeamMember(user.username)
      .orElse {
        nick.flatMap(findSimilarTeamMember)
      }

    relatedMemberMaybe.orElse {
      logger.info(s"No related member found for ${user.username}, nick = ${nick}")
      None
    }

    relatedMemberMaybe
      .foreach { teamMember =>
        logger.info(s"Found potential scammer: ${user.username}, nick = $nick, similar to $teamMember")
        handlePotentialScammer(
          client,
          channel,
          relatedTeamMember = teamMember,
          scammerMention = user.mention
        )
      }
  }

  def sendMessage(client: DiscordClient, channel: GuildChannel, msg: String)(
      implicit c: CacheSnapshot,
      ec: ExecutionContext
  ): Unit = {
    val textChannelId = TextChannelId(channel.id.toString)
    val request = CreateMessage(textChannelId, CreateMessageData(content = msg))
    client.requestsHelper.run(request).value.onComplete {
      case Success(_) => ()
      case Failure(ex) =>
        logger.warn(s"Failed to notify about a potential scammer: $msg", ex)
    }
  }

  private def findSimilarTeamMember(username: String): Option[String] = {
    val lowerCaseUsername = username.toLowerCase
    teamUsernames.find { team =>
      val x = distance(team.toLowerCase, lowerCaseUsername)
      x >= 1 && x <= DistanceThreeshold
    }
  }

  val teamUsernames =
    """
      |AlexITC
      |Draper.x9""".stripMargin.split("\n").map(_.trim).filter(_.nonEmpty)

}
